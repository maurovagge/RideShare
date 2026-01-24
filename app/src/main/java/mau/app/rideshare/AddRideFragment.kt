package mau.app.rideshare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentListBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue



class AddRideFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    //binding object and property
    private var bind: FragmentAddRideBinding? = null
    private val binding get() = bind!!

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        (requireActivity() as? MainActivity)?.hideOptionMenu()

//        //in a new ride driver is the current user
//        //it cannot be changed
//        binding.etAutista.setText(sharedViewModel.currentUser.value?.Nome)
//        binding.etTelefono.setText(sharedViewModel.currentUser.value?.Telefono)

        val button: Button = view.findViewById<Button>(R.id.buttonSave)


        //save the ride to the database (by view model)
        button.setOnClickListener {
            val Ride = Ride()
            Ride.Partenza = binding.etPartenza.text.toString()
            Ride.Arrivo = binding.etArrivo.text.toString()
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
                sharedViewModel.saveRide(Ride)

                //move back to main fragment
                val navController = findNavController()
                navController.navigate(R.id.action_addRideFragment_to_listFragment)
            }
        }

        //show date time picker when clicking on the edit text
        val datetext: TextInputEditText = view.findViewById<TextInputEditText>(R.id.etDataOra)
        datetext.setOnClickListener {
            showDateTimePicker()
        }

    }

    private fun validateRide(ride : Ride): String? {

        if (ride.Partenza.isEmpty())
            return "Partenza non valida"

        if (ride.Arrivo.isEmpty())
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

}


