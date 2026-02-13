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
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.databinding.FragmentAddRideBinding
import mau.app.rideshare.databinding.FragmentDetailBinding
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DetailFragment : Fragment() {

    //binding object and property
    private var bind: FragmentDetailBinding? = null
    private val binding get() = bind!!

    //the view model
    private val sharedViewModel: RideShareViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentDetailBinding.inflate(inflater, container, false)
        binding.viewModel = sharedViewModel
        binding.lifecycleOwner=viewLifecycleOwner
        return binding.root

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as? MainActivity)?.hideOptionMenu()

        //show / hide buttons based on use status
        updateButtons()



        //move back to main fragment
        binding.buttonViewPassengers.setOnClickListener{
            val navController = findNavController()
            navController.navigate(R.id.action_detailFragment_to_viewPassengersFragment)
        }

        //chat fragment
        binding.buttonOpenChat.setOnClickListener {
            findNavController().navigate(R.id.action_rideDetailFragment_to_chatFragment)
        }

        //join ride as passenger
        binding.buttonJoinRide.setOnClickListener {

            if (sharedViewModel.currentRide.value!!.postiLiberi <= 0) {
                Toast.makeText(
                    requireContext(),
                    "I posti su questo viaggio sono terminati",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val user=sharedViewModel.currentUser.value
            if (user?.telefono?.isEmpty() == true || user?.telefono == "null")
            {
                Toast.makeText(requireContext(), "Completa il profilo per aggiungerti ad un viaggio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("CONFERMA")
            builder.setMessage("Sei sicuro di voler partecipare questo viaggio?")
            builder.setPositiveButton("Conferma"){_,_->
                sharedViewModel.joinRide()
                updateButtons()
                Toast.makeText(requireContext(), "Ti  sei unito al viaggio", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("Annulla"){dialog,_->
                dialog.dismiss()
            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()
        }

        //leave ride
        binding.buttonLeaveRide.setOnClickListener {
            val builder= AlertDialog.Builder(requireContext())
            builder.setTitle("CONFERMA")
            builder.setMessage("Sei sicuro di voler abbandonare il viaggio?")
            builder.setPositiveButton("Conferma"){_,_->
                sharedViewModel.leaveRide()
                updateButtons()
                Toast.makeText(requireContext(), "Hai abbandonato il viaggio", Toast.LENGTH_SHORT).show()
            }
            builder.setNegativeButton("Annulla"){dialog,_->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }

        binding.buttonDeleteRide.setOnClickListener {
            val builder= AlertDialog.Builder(requireContext())
            builder.setTitle("CONFERMA")
            builder.setMessage("Sei sicuro di voler cancellare il viaggio?")
            builder.setPositiveButton("Conferma"){_,_->
                sharedViewModel.deleteRide()
                updateButtons()
                Toast.makeText(requireContext(), "Hai cancellato il viaggio", Toast.LENGTH_SHORT).show()
                val navController = findNavController()
                navController.navigateUp()
            }
            builder.setNegativeButton("Annulla"){dialog,_->
                dialog.dismiss()
            }
            val alertDialog = builder.create()
            alertDialog.show()
        }

        binding.buttonOpenMap.setOnClickListener {
            val bundle = Bundle().apply {
                putString("rideId", sharedViewModel.currentRide.value?.id)
                putString("soSId", null)
            }
            findNavController().navigate(R.id.action_rideDetailFragment_to_mapFragment, bundle)
        }

//        binding.buttonOpenMap.setOnClickListener {
//            val uriString =  "myapp://sos_detail/DQveAFbXPRZsR8TGsBAJ"
//
//            val navController = findNavController()
////            navController.navigate(uriString.toUri())
//        }


    }

    fun updateButtons(){
        val user=sharedViewModel.currentUser.value

        // if I am the owner/driver I cannot join as passenger
        val isUserJoined = sharedViewModel.currentRide.value?.viaggiatori?.any { it.userid.equals(user?.id, ignoreCase = true) } ?: false
        val isUserDriver = (sharedViewModel.currentRide.value?.autista == user?.id) ?: false
        if (isUserDriver) {
            binding.buttonJoinRide.visibility = View.GONE
            binding.buttonLeaveRide.visibility = View.GONE
            binding.buttonOpenChat.visibility= View.VISIBLE
            binding.buttonDeleteRide.visibility = View.VISIBLE

        }
        else {
            if (isUserJoined) {
                // already joined
                binding.buttonJoinRide.visibility = View.GONE
                binding.buttonLeaveRide.visibility = View.VISIBLE
                binding.buttonOpenChat.visibility= View.VISIBLE
                binding.buttonDeleteRide.visibility = View.GONE
            } else {
                // not joined
                binding.buttonJoinRide.visibility = View.VISIBLE
                binding.buttonLeaveRide.visibility = View.GONE
                binding.buttonOpenChat.visibility= View.GONE
                binding.buttonDeleteRide.visibility = View.GONE
            }
        }
    }
}