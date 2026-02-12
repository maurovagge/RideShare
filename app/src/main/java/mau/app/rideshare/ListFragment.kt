package mau.app.rideshare

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import mau.app.rideshare.databinding.FragmentListBinding
import kotlin.getValue

class ListFragment : Fragment() {

    //binding object and property
    private var bind: FragmentListBinding? = null
    private val binding get() = bind!!

    //the view model
    private val sharedViewModel: RideShareViewModel by activityViewModels()

    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentListBinding.inflate(inflater, container, false)
        binding.viewModel = sharedViewModel

        binding.rv.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //show action bar
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }
        (activity as? AppCompatActivity)?.supportActionBar?.show()

        (requireActivity() as? MainActivity)?.showOptionMenu()

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(requireContext())

        // Exit app in case of back button pressed
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finishAffinity()
                }
            })


        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all -> {
                    sharedViewModel.changeRideList(false)
                    true
                }

                R.id.nav_mine -> {
                    sharedViewModel.changeRideList(true)
                    true
                }

                else -> false
            }
        }

        //create adapter for the recycler view and set the onClick function
        val adapter = ListBindingAdapter { ride ->
            sharedViewModel.currentRide.value = ride
            val navController = findNavController()
//            navController.navigate(R.id.action_listFragment_to_detailFragment)
            val bundle = Bundle().apply {
                putString("rideId", ride.id)
            }
            navController.navigate(R.id.action_listFragment_to_ridePagerFragment, bundle)
        }

        //  val searchButton: Button = view.findViewById<Button>(R.id.btnSearch)
        val searchSection: LinearLayout = view.findViewById<LinearLayout>(R.id.searchSection)

//        searchButton.setOnClickListener { view ->
//            searchSection.visibility = View.GONE
//        }

        binding.labelSearchPartenza.setEndIconOnClickListener {
            binding.txtSearchPartenza.text?.clear()

            binding.txtSearchPartenza.setText("")
            sharedViewModel.changeSearchCoords(LatLng(0.0, 0.0))
            binding.labelSearchPartenza.isEndIconVisible = false
            setDistanceVisibility(false)

        }

        binding.txtSearchPartenza.addTextChangedListener {
            binding.labelSearchPartenza.isEndIconVisible = it?.isNotEmpty() == true
        }


        //bind the adapter to the view
        binding.rv.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner
        binding.rv.layoutManager = LinearLayoutManager(requireContext())

        val searchPartenza: TextInputEditText =
            view.findViewById<TextInputEditText>(R.id.txtSearchPartenza)
        searchPartenza.setOnClickListener {
            val fields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LOCATION,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.ADDRESS
            )


            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, // Usa FULLSCREEN o OVERLAY
                fields
            ).build(requireContext())
            autocompleteLauncherSearch.launch(intent)
        }
        if (searchPartenza.text != null) {
            if (searchPartenza.text!!.isEmpty()) {
                setDistanceVisibility(false)
            } else {
                setDistanceVisibility(true)
            }
        }

        binding.distanceSlider.addOnChangeListener { slider, value, fromUser ->
            binding.textDistance.text = "${value.toInt()} km"
        }


        binding.distanceSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
            }
            override fun onStopTrackingTouch(slider: Slider) {
                sharedViewModel.changeSearchDistance(slider.value.toInt()*1000)
            }
        })
    }

    private val autocompleteLauncherSearch =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    val latLng = place.location

                    val fullName = NotificationUtil.getPlaceName(place)
                    binding.txtSearchPartenza.setText(fullName)
                    if (latLng != null) {
                        sharedViewModel.changeSearchCoords(latLng)
                        setDistanceVisibility(true)
                    }
                    Log.i("PlacesApp", "Luogo selezionato: ${place.name}, LatLng: ${place.latLng}")
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.d("PlacesApp", "Ricerca annullata dall'utente.")
            }
        }

    private fun setDistanceVisibility (visible : Boolean) {
        if (visible) {
            binding.labelSearchPartenza.isEndIconVisible = true
            binding.distanceSlider.visibility = View.VISIBLE
            binding.textDistance.visibility = View.VISIBLE
        } else {
            binding.labelSearchPartenza.isEndIconVisible = false
            binding.distanceSlider.visibility = View.GONE
            binding.textDistance.visibility = View.GONE
        }
    }
}