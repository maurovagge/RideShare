package mau.app.rideshare

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
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentRideDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RideDetailFragment : Fragment() {

    //binding object and property
    private var bind: FragmentRideDetailBinding? = null
    private val binding get() = bind!!

    private var rideId: String? = null

    private val rideDetailViewModel: RideDetailViewModel by viewModels()


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

        if (rideId != null) {
            rideDetailViewModel.observeRide(rideId!!)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            rideDetailViewModel.rideState.collect { status ->
                if (status != null) {
                    binding.chipTripStatus.text = status?.Stato

                    // Aggiorna il testo del bottone in base allo stato successivo
                    val nextStatus = rideDetailViewModel.getNextStatus()
                    if (nextStatus != null) {
                        binding.btnNextStatus.text = "Passa a ${nextStatus}"
                    } else {
                        binding.driverActionPanel.isVisible = false // Viaggio terminato
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
            rideDetailViewModel.moveToNextStatus()
        }
    }
}