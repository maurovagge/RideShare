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
                    // Update passenger check-in status and general ride info
                    adapter.updateCheckinStatus(ride.viaggiatori)
                    binding.chipTripStatus.text = ride.stato
                    binding.tvDeparture.text = ride.partenza.Address
                    binding.tvArrival.text = ride.arrivo.Address

                    // Time formatter for the timeline
                    val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

                    // Set estimated departure time on the timeline
                    ride.partenza.EstimatedTime?.let { departureTimestamp ->
                        binding.tvDepartureTime.text = timeSdf.format(departureTimestamp.toDate())
                    }

                    // Set estimated arrival time on the timeline
                    ride.arrivo.EstimatedTime?.let { arrivalTimestamp ->
                        binding.tvArrivalTime.text = timeSdf.format(arrivalTimestamp.toDate())
                    }

                    // Format the main date display
                    ride.data?.let { timestamp ->
                        val date = timestamp.toDate()

                        // Use DateUtils to get a relative string (Today, Tomorrow) or a formatted date
                        val relativeDate = android.text.format.DateUtils.getRelativeTimeSpanString(
                            date.time,
                            System.currentTimeMillis(),
                            android.text.format.DateUtils.DAY_IN_MILLIS
                        ).toString()

                        // If it's not "Today" or "Tomorrow", use full date format
                        if (relativeDate.any { it.isDigit() }) {
                            val dateSdf = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault())
                            binding.tvDateTime.text = dateSdf.format(date).replaceFirstChar { it.uppercase() }
                        } else {
                            binding.tvDateTime.text = relativeDate.replaceFirstChar { it.uppercase() }
                        }
                    }

                    // Driver action panel logic: update button label based on next state
                    val nextStatusActionLabel = rideDetailViewModel.getNextStatusActionLabel()
                    if (nextStatusActionLabel.isNotEmpty()) {
                        binding.btnNextStatus.text = nextStatusActionLabel
                        binding.driverActionPanel.isVisible = true
                    } else {
                        // Hide panel if the ride is finished or no further actions are available
                        binding.driverActionPanel.isVisible = false
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
            combine(
                rideDetailViewModel.isUserDriver,
                rideDetailViewModel.isUserJoined,
                rideDetailViewModel.rideState
            ) { isDriver, isJoined, ride ->
                Triple(isDriver, isJoined, ride)
            }.collect { (isDriver, isJoined, ride) ->
                if (ride == null) return@collect

                // control panels
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

                    // passengers button
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

        // show/hide driver control panel
        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.isDriver.collect { isDriver ->
                binding.driverActionPanel.isVisible = isDriver
            }
        }

        // asking for driver confirmation and changing ride state
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
                "OK" -> { // no action
                }
                else -> {
                    message = "Sei sicuro di voler passare a ${rideDetailViewModel.getNextStatusLabel()}?"
                    alert.setMessage(message).show()
                }
            }
        }

        // join ride
        binding.buttonJoinRide.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("CONFERMA")
                .setMessage("Sei sicuro di voler partecipare a questo viaggio?")
                .setPositiveButton("Conferma") { _, _ ->
                    rideDetailViewModel.joinRide()
                    Toast.makeText(requireContext(), "Ti sei unito al viaggio", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Annulla", null)
                .show()
        }

        //leave ride
        binding.buttonLeaveRide.setOnClickListener {
            // creating confirmation toast
            AlertDialog.Builder(requireContext())
                .setTitle("CONFERMA")
                .setMessage("Sei sicuro di voler abbandonare il viaggio?")
                .setPositiveButton("Conferma") { _, _ ->
                    // calling leave function
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