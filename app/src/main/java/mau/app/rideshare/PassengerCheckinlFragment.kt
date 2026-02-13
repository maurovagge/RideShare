package mau.app.rideshare

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
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentDriverCheckinBinding
import mau.app.rideshare.databinding.FragmentPassengerCheckinBinding
import mau.app.rideshare.databinding.FragmentRideDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PassengerCheckinlFragment : Fragment() {

    //binding object and property
    private var bind: FragmentPassengerCheckinBinding? = null
    private val binding get() = bind!!

    private var rideId: String? = null

    private val passengerCheckinViewModel: PassengerCheckinViewModel by viewModels()


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
        bind = FragmentPassengerCheckinBinding.inflate(inflater, container, false)
        binding.lifecycleOwner=viewLifecycleOwner
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? MainActivity)?.hideOptionMenu()

        val scanner = GmsBarcodeScanning.getClient(requireContext())

        binding.btnStartScan.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (rawValue != null) {
                        passengerCheckinViewModel.performCheckIn( rawValue)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Scansione annullata", Toast.LENGTH_SHORT).show()
                }
        }

        passengerCheckinViewModel.checkInStatus.observe(viewLifecycleOwner) { isTrue ->
            if (isTrue) {
                Toast.makeText(context, "Check In Effettuato", Toast.LENGTH_SHORT).show()
            }
            else {
            }
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }
}