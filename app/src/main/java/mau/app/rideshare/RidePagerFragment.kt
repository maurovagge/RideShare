package mau.app.rideshare

import android.R.attr.fragment
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import mau.app.rideshare.databinding.FragmentRidePagerBinding
import kotlin.getValue

class RidePagerFragment : Fragment() {

    private var _binding: FragmentRidePagerBinding? = null
    private val binding get() = _binding!!

    private var mediator: TabLayoutMediator? = null
    private val rideDetailViewModel: RideDetailViewModel by activityViewModels()

    private val rideId: String by lazy {
        arguments?.getString("rideId") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRidePagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (rideId.isEmpty()) return

        rideDetailViewModel.observeRide(rideId)
        val adapter = RidePagerAdapter(this, rideId)
        binding.viewPager.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {

            combine(
                rideDetailViewModel.isUserDriver,
                rideDetailViewModel.isUserJoined
            ) { isDriver, isJoined -> Pair(isDriver, isJoined) }
                .collect { (isDriver, isJoined) ->

                    // Calcolo del numero di pagine
                    val newCount = when {
                        isDriver -> 4
                        isJoined -> 4
                        else -> 2
                    }

                    // mediator reset (to redraw tab pager)
                    if (adapter.currentItemCount != newCount) {
                        adapter.currentItemCount = newCount
                        adapter.isDriver = isDriver
                        binding.viewPager.adapter = adapter
                        mediator?.detach()
                        mediator = null
                    }

                    if (mediator == null) {
                        mediator = TabLayoutMediator(
                            binding.tabLayout,
                            binding.viewPager
                        ) { tab, position ->
                            tab.text = when (position) {
                                0 -> "DETTAGLI"
                                1 -> "MAPPA"
                                2 -> "CHAT"
                                3 -> "CHECK-IN"
                                else -> null
                            }
                        }
                        mediator?.attach()
                    }

                    //pop back to first tab
                    if (binding.viewPager.currentItem >= newCount) {
                        binding.viewPager.setCurrentItem(0, false)
                    }
                }
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Swipe disabled on map
                binding.viewPager.isUserInputEnabled = (position != 1)
            }
        })
    }

    class RidePagerAdapter(fragment: Fragment, private val rideId: String) :
        FragmentStateAdapter(fragment) {
        var currentItemCount = 2 // everybody can see ride detail and map
        var isDriver = false


        override fun getItemCount(): Int = currentItemCount

        override fun createFragment(position: Int): Fragment {
            val args = Bundle().apply { putString("rideId", rideId) }
            return when (position) {
                0 -> RideDetailFragment().apply { arguments = args }
                1 -> MapFragment().apply { arguments = args }
                2 -> ChatFragment().apply { arguments = args }
                3 -> if (isDriver)
                    DriverCheckinlFragment().apply { arguments = args }
                else {
                    PassengerCheckinlFragment().apply { arguments = args }
                }

                else -> RideDetailFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}