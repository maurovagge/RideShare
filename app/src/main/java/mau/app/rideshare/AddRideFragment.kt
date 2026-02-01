package mau.app.rideshare

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import mau.app.rideshare.databinding.FragmentAddRideBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.getValue



class AddRideFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    //binding object and property
    private var bind: FragmentAddRideBinding? = null
    private val binding get() = bind!!

    private var currentRide : Ride? = null
    //the view model
    private val sharedViewModel: RideShareViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        bind = FragmentAddRideBinding.inflate(inflater, container, false)
        binding.viewModel=sharedViewModel
        binding.lifecycleOwner=viewLifecycleOwner
        return binding.root
    }

    private lateinit var placesClient: PlacesClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        (requireActivity() as? MainActivity)?.hideOptionMenu()


        // 1. Inizializza Places usando il context del fragment
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(requireContext())


//        //in a new ride driver is the current user
//        //it cannot be changed
//        binding.etAutista.setText(sharedViewModel.currentUser.value?.Nome)
//        binding.etTelefono.setText(sharedViewModel.currentUser.value?.Telefono)

        val button: Button = view.findViewById<Button>(R.id.buttonSave)


        //save the ride to the database (by view model)
        button.setOnClickListener {
            val Ride = Ride()
            Ride.Partenza.Address = binding.etPartenza.text.toString()
            Ride.Arrivo.Address = binding.etArrivo.text.toString()
            Ride.Autista = sharedViewModel.currentUser.value?.id.toString()
            if (sharedViewModel.convertDateStringToTimestamp(binding.etDataOra.text.toString()) != null) {
                Ride.Data = sharedViewModel.convertDateStringToTimestamp(binding.etDataOra.text.toString())!!
            }
            Ride.Telefono = sharedViewModel.currentUser.value?.Telefono.toString()

            Ride.Posti = binding.etPosti.text.toString().toIntOrNull() ?: 0

            var msg : String? = null
            msg = validateRide(Ride)

            if (msg != null) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT)
                    .show()
            }
            else {
                Ride.Partenza.AddressCoords = partenzaCoords
                Ride.Arrivo.AddressCoords = arrivoCoords

                currentRide = Ride;
                sharedViewModel.CalculateRideRouteTime(Ride.Partenza.AddressCoords, Ride.Arrivo.AddressCoords)
            }
        }

        val partenza: TextInputEditText = view.findViewById<TextInputEditText>(R.id.etPartenza)
        partenza.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LOCATION, Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS)


            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, // Usa FULLSCREEN o OVERLAY
                fields
            ).build(requireContext())
            autocompleteLauncherPartenza.launch(intent)

        }

        val arrivo: TextInputEditText = view.findViewById<TextInputEditText>(R.id.etArrivo)
        arrivo.setOnClickListener {
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LOCATION, Place.Field.ADDRESS_COMPONENTS, Place.Field.ADDRESS)


            val intent = Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, // Usa FULLSCREEN o OVERLAY
                fields
            ).build(requireContext())
            autocompleteLauncherArrivo.launch(intent)

        }

        // 1. Osserva il tempo stimato
        sharedViewModel.tempoStimato.observe(viewLifecycleOwner) { tempo ->

            currentRide!!.Partenza.EstimatedTime = currentRide!!.Data

            currentRide!!.Arrivo.EstimatedTime = currentRide!!.Data.addSeconds(sharedViewModel.tempoStimato.value!!)




            sharedViewModel.saveRide(currentRide)

            //move back to main fragment
            val navController = findNavController()
            navController.navigate(R.id.action_addRideFragment_to_listFragment)
        }


        //show date time picker when clicking on the edit text
        val datetext: TextInputEditText = view.findViewById<TextInputEditText>(R.id.etDataOra)
        datetext.setOnClickListener {
            showDateTimePicker()
        }

    }

    fun Timestamp.addSeconds(seconds: Long): Timestamp {
        val currentMillis = this.toDate().time
        val futureMillis = currentMillis + (seconds * 1000L)
        return Timestamp(Date(futureMillis))
    }


    private val autocompleteLauncherPartenza =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // L'utente ha selezionato un luogo con successo
                val intent = result.data
                if (intent != null) {
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    val latLng = place.location
                    partenzaCoords = latLng.toRideShareLocation()
                    val fullName = getPlaceName(place)
                    binding.etPartenza.setText(fullName)

                    Log.i("PlacesApp", "Luogo selezionato: ${place.name}, LatLng: ${place.latLng}")
                    // place.latLng contiene le coordinate esatte
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // L'utente ha chiuso la schermata di ricerca senza selezionare nulla
                Log.d("PlacesApp", "Ricerca annullata dall'utente.")
            }
        }

    lateinit var partenzaCoords : RideShareLocation
    lateinit var arrivoCoords : RideShareLocation

    private val autocompleteLauncherArrivo =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // L'utente ha selezionato un luogo con successo
                val intent = result.data
                if (intent != null) {
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    val latLng = place.location
                    arrivoCoords = latLng.toRideShareLocation()
                    val fullName = getPlaceName(place)
                    binding.etArrivo.setText(fullName)

                    Log.i("PlacesApp", "Luogo selezionato: ${place.name}, LatLng: ${place.latLng}")
                    // place.latLng contiene le coordinate esatte
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // L'utente ha chiuso la schermata di ricerca senza selezionare nulla
                Log.d("PlacesApp", "Ricerca annullata dall'utente.")
            }
        }

    private fun validateRide(ride : Ride): String? {

        if (ride.Partenza.Address.isEmpty())
            return "Partenza non valida"

        if (ride.Arrivo.Address.isEmpty())
            return "Arrivo non valido"

        if (ride.Data == null)
            return "Data non valida"

        if (ride.Posti <= 0)
            return "Specificare almeno un posto"

        if (ride.Data < Timestamp.now())
            return "Non è possibile creare un viaggio nel passato"

        if (ride.Telefono.isEmpty() || ride.Telefono == "null")
            return "Completa il profilo prima di creare un viaggio"

        return null
    }


    //show date time picker
    private fun showDateTimePicker() {
        val calendar= Calendar.getInstance()


        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleziona Data")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        //on date confirmation, show time picker
        datePicker.addOnPositiveButtonClickListener { dataSelezionata ->
            calendar.timeInMillis = dataSelezionata

            // create time picker
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Seleziona Orario")
                .build()

            //on time confirmation, set the result in the edit text
            timePicker.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)

                //(convert to string)
                val formatoFinale = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                binding.etDataOra.setText(formatoFinale.format(calendar.time))
            }
            timePicker.show(parentFragmentManager , "TIME_PICKER")
        }
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }
    private fun getPlaceName(place: Place) : String
    {
        var fullName = ""
        if (place.addressComponents != null) {
            val components = place.addressComponents?.asList()


            var via = ""
            var civico = ""
            var comune = ""

            for (component in components!!) {
                val types = component.types

                when {
                    // Via/Strada
                    types.contains("route") -> via = component.name

                    // Numero Civico
                    types.contains("street_number") -> civico = component.name

                    // Comune (Locality)
                    types.contains("locality") -> comune = component.name
                }
            }
            if (comune.isNotEmpty())
            {
                fullName += comune
                if (via.isNotEmpty())
                {
                    fullName += ", $via"
                }
                if (civico.isNotEmpty())
                {
                    fullName += " $civico"
                }
            }
            else
            {
                fullName = place.displayName
            }
            return fullName
        }
        return fullName
    }


}


