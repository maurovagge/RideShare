package mau.app.rideshare

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
    // RECUPERO MANUALE (Sostituisce args)
    private val rideId: String by lazy {
        arguments?.getString("rideId") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRidePagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ora usiamo la variabile rideId locale
        if (rideId.isEmpty()) return

        rideDetailViewModel.observeRide(rideId)
        val adapter = RidePagerAdapter(this, rideId)
        binding.viewPager.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                rideDetailViewModel.isUserDriver,
                rideDetailViewModel.isUserJoined
            ) { isUserDriver, isUserJoined ->
                isUserDriver || isUserJoined // L'utente è autorizzato se è autista O passeggero
            }.collect { canAccessChat ->
                binding.viewPager.isUserInputEnabled = canAccessChat

                if (canAccessChat) {
                    binding.tabLayout.visibility = View.VISIBLE
                    // Riattacchiamo il mediatore se non è già attivo
                    if (mediator == null) {
                        mediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                            tab.text = if (position == 0) "DETTAGLI" else "CHAT"
                        }
                        mediator?.attach()
                    }
                } else {
                    binding.tabLayout.visibility = View.GONE
                    mediator?.detach()
                    mediator = null
                    binding.viewPager.currentItem = 0
                }
            }
        }
    }
    class RidePagerAdapter(fragment: Fragment, private val rideId: String) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RideDetailFragment().apply {
                    arguments = Bundle().apply { putString("rideId", rideId) }
                }
                else -> ChatFragment().apply {
                    arguments = Bundle().apply { putString("rideId", rideId) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}