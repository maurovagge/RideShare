package mau.app.rideshare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.databinding.FragmentListBinding
import kotlin.getValue

class ListFragment : Fragment() {

    //binding object and property
    private var bind: FragmentListBinding? = null
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


        // Exit app in case of back button pressed
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finishAffinity()
                }
            })

        //create adapter for the recycler view and set the onClick function
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
        val adapter = ListBindingAdapter { ride ->
            sharedViewModel.currentRide.value = ride
            val navController = findNavController()
//            navController.navigate(R.id.action_listFragment_to_detailFragment)
            val bundle = Bundle().apply {
                putString("rideId", ride.id)
            }
            //           navController.navigate(R.id.action_listFragment_to_rideDetailFragment, bundle)
            navController.navigate(R.id.action_listFragment_to_driverCheckinFragment, bundle)
        }

        val searchButton: Button = view.findViewById<Button>(R.id.btnSearch)
        val searchSection: LinearLayout = view.findViewById<LinearLayout>(R.id.searchSection)

        searchButton.setOnClickListener { view ->
            searchSection.visibility = View.GONE
        }

        //bind the adapter to the view
        binding.rv.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner
        binding.rv.layoutManager = LinearLayoutManager(requireContext())


    }

}