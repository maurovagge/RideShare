package mau.app.rideshare

import UserAdapter
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentDriverCheckinBinding
import mau.app.rideshare.databinding.FragmentRideDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DriverCheckinlFragment : Fragment() {

    //binding object and property
    private var bind: FragmentDriverCheckinBinding? = null
    private val binding get() = bind!!

    private var rideId: String? = null

    private val driverCheckinViewModel: DriverCheckinViewModel by viewModels()

    private var listCheckin : List<String> = emptyList()

    val adapter = UserAdapter(emptyList())



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            rideId = it.getString("rideId")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentDriverCheckinBinding.inflate(inflater, container, false)
        binding.viewModel = driverCheckinViewModel
        binding.lifecycleOwner=viewLifecycleOwner
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? MainActivity)?.hideOptionMenu()

        binding.rvPassengers.adapter = adapter
        binding.rvPassengers.layoutManager = LinearLayoutManager(requireContext())

        if (rideId != null) {
            driverCheckinViewModel.observeRide(rideId!!)

            val qrBitmap = generateQRCode(rideId!!)
            binding.ivQrCode.setImageBitmap(qrBitmap)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            driverCheckinViewModel.rideState.collect { ride ->
                if (ride != null) {
                    if (ride.checkin.count() != listCheckin.count()) {
                        listCheckin = ride.checkin
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            driverCheckinViewModel.passengersState.collect { listaPasseggeri ->
                adapter.updateData(listaPasseggeri)
            }
        }
    }




    private fun checkinConfirm() {
        // Mostra un feedback all'autista e chiudi il fragment
        Toast.makeText(context, "CheckIn effettuato con successo", Toast.LENGTH_LONG).show()
        parentFragmentManager.popBackStack()
    }

    private fun generateQRCode(text: String): Bitmap {
        val width = 500
        val height = 500
        val encoder = BarcodeEncoder()
        // Genera una matrice di bit e la converte in Bitmap
        return encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, width, height)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }
}