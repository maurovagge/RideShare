package mau.app.rideshare

import UserAdapter
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentRideDetailBinding
import kotlinx.coroutines.flow.combine
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RideDetailFragment : Fragment() {

    //binding object and property
    private var bind: FragmentRideDetailBinding? = null
    private val binding get() = bind!!

    private var rideId: String? = null

    private val rideDetailViewModel: RideDetailViewModel by viewModels()

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
        bind = FragmentRideDetailBinding.inflate(inflater, container, false)
        binding.viewModel = rideDetailViewModel
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
            rideDetailViewModel.observeRide(rideId!!)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.rideState.collect { ride ->
                if (ride != null) {
                    adapter.updateCheckinStatus(ride.viaggiatori)
                    binding.chipTripStatus.text = ride?.stato
                    binding.tvDeparture.text=ride.partenza.Address
                    binding.tvArrival.text=ride.arrivo.Address
                    ride.data?.let { timestamp ->
                        val date = timestamp.toDate() // Converte Timestamp in Date
                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                        binding.tvDateTime.text = sdf.format(date)
                    }

                    // Aggiorna il testo del bottone in base allo stato successivo
                    val nextStatusActionLabel = rideDetailViewModel.getNextStatusActionLabel()
                    if (nextStatusActionLabel.isNotEmpty()) {
                        binding.btnNextStatus.text = nextStatusActionLabel
                    } else {
                        binding.driverActionPanel.isVisible = false // Viaggio terminato
                    }
                }
            }

        }

        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.passengersState.collect { listaPasseggeri ->
                adapter.updateData(listaPasseggeri)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.driverState.collect { driver ->
                if (driver != null) {
                    binding.tvDriverName.text = driver.nome
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Il "combine" reagisce non appena cambia uno dei tre flussi
            combine(
                rideDetailViewModel.isUserDriver,
                rideDetailViewModel.isUserJoined,
                rideDetailViewModel.rideState
            ) { isDriver, isJoined, ride ->
                // Creiamo un pacchetto di dati aggiornati
                Triple(isDriver, isJoined, ride)
            }.collect { (isDriver, isJoined, ride) ->
                if (ride == null) return@collect

                // 1. GESTIONE PANNELLI PRINCIPALI
                if (isDriver) {
                    if (ride.stato!="Terminato"){
                        binding.driverActionPanel.visibility = View.VISIBLE
                    }
                    else{
                        binding.driverActionPanel.visibility = View.GONE
                    }
                    binding.passengerActionPanel.visibility = View.GONE
                } else {
                    binding.driverActionPanel.visibility = View.GONE
                    binding.passengerActionPanel.visibility = View.VISIBLE

                    // 2. GESTIONE TASTI PASSEGGERO
                    if (isJoined) {
                        binding.buttonJoinRide.visibility = View.GONE
                        binding.buttonLeaveRide.visibility = View.VISIBLE
                    } else {
                        binding.buttonJoinRide.visibility = View.VISIBLE
                        binding.buttonLeaveRide.visibility = View.GONE
                    }
                }
            }
        }

        // Mostra/Nascondi pannello autista
        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.isDriver.collect { isDriver ->
                binding.driverActionPanel.isVisible = isDriver
            }
        }

        binding.btnNextStatus.setOnClickListener {
            val nextStatusResult = rideDetailViewModel.checkNextStatus()

            val alert = MaterialAlertDialogBuilder(requireContext())
                .setTitle("CONFERMA")
                .setNegativeButton("Annulla") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Conferma") { _, _ ->
                    rideDetailViewModel.moveToNextStatus()
                }
            var message = ""
            when (nextStatusResult) {
                "WarningImbarco" -> {
                    message = "Stai aprendo l'imbarco con notevole anticipo. Sei sicuro?"
                    alert.setMessage(message).show()
                }
                "WarningInizio" -> {
                    message = "Stai iniziando il viaggio con notevole anticipo. Sei sicuro?"
                    alert.setMessage(message).show()
                }
                "OK" -> { /* Nessuna azione */
                }
                else -> {
                    message = "Sei sicuro di voler passare a ${rideDetailViewModel.getNextStatusLabel()}?"
                    alert.setMessage(message).show()
                }
            }
        }

        binding.buttonJoinRide.setOnClickListener {
            // 1. Controllo profilo (puoi recuperare l'utente dal driverState o da un nuovo state)
            // Se non hai i dati dell'utente loggato pronti, puoi saltare questo check o implementarlo dopo

            AlertDialog.Builder(requireContext())
                .setTitle("CONFERMA")
                .setMessage("Sei sicuro di voler partecipare a questo viaggio?")
                .setPositiveButton("Conferma") { _, _ ->
                    // Chiamiamo il NUOVO ViewModel
                    rideDetailViewModel.joinRide()
                    Toast.makeText(requireContext(), "Ti sei unito al viaggio", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        //leave ride
        binding.buttonLeaveRide.setOnClickListener {
            // Creiamo il dialogo di conferma (come nelle tue vecchie immagini)
            AlertDialog.Builder(requireContext())
                .setTitle("CONFERMA")
                .setMessage("Sei sicuro di voler abbandonare il viaggio?")
                .setPositiveButton("Conferma") { _, _ ->
                    // Chiamiamo la funzione nel nuovo ViewModel
                    rideDetailViewModel.leaveRide()

                    Toast.makeText(
                        requireContext(),
                        "Hai abbandonato il viaggio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Annulla") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        binding.buttonDeleteRide.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("CONFERMA")
                .setMessage("Sei sicuro di voler cancellare il viaggio?")
                .setPositiveButton("Conferma") { _, _ ->
                    rideDetailViewModel.deleteRide()
                    Toast.makeText(requireContext(), "Hai cancellato il viaggio", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp() // Torna indietro dopo l'eliminazione
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }




}