package mau.app.rideshare

import ChatViewModel
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.AdvancedMarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PinConfig
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.SquareCap
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentMapBinding
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MapFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var rideId: String? = null

    private var sosId: String? = null

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val mapViewModel: MapViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            rideId = it.getString("rideId")
            sosId = it.getString("sosId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_map, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.mapVehicleDetailCard.visibility = View.GONE

        binding.mapVehicleStatus.text = "..."

        // Usa childFragmentManager perché il fragment della mappa è dentro questo Fragment
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment?.getMapAsync(this)

        if (rideId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mapViewModel.routePoints.collect { points ->
                        mapViewModel.listaPunti = points
                        if (mapViewModel.rideState.value != null) {
                            drawRide(mapViewModel.rideState.value!!)
                        }
                    }
                }
            }
        }

        if (sosId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mapViewModel.sosState.collect { sos ->
                        sos?.let {
                            drawSOS(it)
                        }
                    }
                }
            }
        }
    }

    private lateinit var sosMarker: Marker

    @SuppressLint("PotentialBehaviorOverride")
    private fun drawSOS(sos: SOS) {
        if (sos.State == "ON") {
            binding.apply {
                mapVehicleDetailCard.visibility = View.VISIBLE
                mapVehicleTitle.text = "SOS"
                mapVehicleTitle.setTextColor(Color.BLACK)
                mapVehicleStatus.text = "Richiesta SOS da: ${sos.SourceUser}"
                mapVehicleStatus.setTextColor(Color.BLACK)
                mapVehicleDetailCard.setCardBackgroundColor(Color.parseColor("#F00000"))
            }
        } else {
            binding.apply {
                mapVehicleDetailCard.visibility = View.VISIBLE
                mapVehicleTitle.text = "SOS TERMINATO"
                mapVehicleTitle.setTextColor(Color.BLACK)
                mapVehicleStatus.text = "SOS Terminato"
                mapVehicleStatus.setTextColor(Color.BLACK)
                mapVehicleDetailCard.setCardBackgroundColor(Color.parseColor("#00F000"))
            }
        }

        if (::sosMarker.isInitialized) {
            sosMarker.position = LatLng(sos.SOSLocation.latitude, sos.SOSLocation.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosMarker.position, 20f))
            return
        }

        val pinConfigFrom = PinConfig.builder()
            .setBackgroundColor(Color.GREEN)
            .setBorderColor(Color.BLACK)
            .setGlyph(PinConfig.Glyph("SOS"))
            .build()
        pinConfigFrom.glyph

        var latLng = LatLng(sos.SOSLocation.latitude, sos.SOSLocation.longitude);

        sosMarker = mMap.addMarker(
            AdvancedMarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromPinConfig(pinConfigFrom))
                .title("SOS")
        )!!
//        sosMarker.tag = "AGGIUNGERE QUI LE INFO"
//        mMap.setOnMarkerClickListener { marker ->
//            binding.mapVehicleDetailCard.visibility = VISIBLE
//
//            binding.mapVehicleDetailCard.alpha = 0f
//            binding.mapVehicleDetailCard.animate().alpha(1f).setDuration(300).start()
//
//            false // false permette il comportamento standard (centra marker)


//            val info = marker.tag
//
//            marker.title = "EMERGENZA:"
//            marker.snippet = info.toString()
//            marker.showInfoWindow()

        //true
//        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sosMarker.position, 20f))

    }

    private fun drawRide(ride: Ride) {
        val partenza = ride.Partenza.AddressCoords
        val arrivo = ride.Arrivo.AddressCoords


        var FromLatLng = LatLng(partenza.latitude, partenza.longitude);
        var ToLatLng = LatLng(arrivo.latitude, arrivo.longitude);


        mMap.clear()

        val pinConfigFrom = PinConfig.builder()
            .setBackgroundColor(Color.GREEN)
            .setBorderColor(Color.BLACK)
            .setGlyph(PinConfig.Glyph("1"))
            .build()
        pinConfigFrom.glyph

        val markerFrom = mMap.addMarker(
            AdvancedMarkerOptions()
                .position(FromLatLng!!)
                .icon(BitmapDescriptorFactory.fromPinConfig(pinConfigFrom))
                .title("Partenza")
        )


        val pinConfigTo = PinConfig.builder()
            .setBackgroundColor(Color.RED)
            .setBorderColor(Color.BLACK)
            .setGlyph(PinConfig.Glyph("2"))
            .build()

        val markerTo = mMap.addMarker(
            AdvancedMarkerOptions()
                .position(ToLatLng!!)
                .icon(BitmapDescriptorFactory.fromPinConfig(pinConfigTo))
                .title("Arrivo")
        )


        val builder = LatLngBounds.Builder()
        FromLatLng?.let {
            builder.include(it)
        }
        ToLatLng?.let {
            builder.include(it)
        }

        val latLngBounds = builder.build()
        val paddingInDp = 64
        val paddingInPx = (paddingInDp * resources.displayMetrics.density).toInt()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, paddingInPx))


        val points = listOf(
            FromLatLng,
            ToLatLng
        )

        if (mapViewModel.listaPunti.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .addAll(mapViewModel.listaPunti)
                .color(Color.BLUE)
                .width(10f)

            mMap.addPolyline(polylineOptions)
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //Center map in an  arbitrary point (Unige :)
        val init = LatLng(44.40350, 8.95843)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(init, 10f))

        mMap.uiSettings.isZoomControlsEnabled = true

        if (rideId != null) {
            mapViewModel.observeRide(rideId!!)
        }
        if (sosId != null) {
            mapViewModel.observeSOS(sosId!!)
        }
//        mMap.setOnMapClickListener { marker ->
//            binding.mapVehicleDetailCard.visibility = View.GONE
//            // Se vuoi, puoi anche far centrare la mappa sul marker
//            false // Ritorna false per mantenere il comportamento standard
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}