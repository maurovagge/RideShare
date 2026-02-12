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

                    // 1. Determiniamo il numero di pagine reali
                    val newCount = when {
                        isDriver -> 4   // DETTAGLI, MAPPA, CHAT, CHECK-IN
                        isJoined -> 3   // DETTAGLI, MAPPA, CHAT
                        else -> 2       // DETTAGLI, MAPPA (Chat e Check-in spariscono)
                    }

                    // 2. Se il conteggio cambia, resettiamo TUTTO
                    if (adapter.currentItemCount != newCount) {
                        adapter.currentItemCount = newCount
                        adapter.notifyDataSetChanged() // Notifica l'adapter del cambio

                        // RESET DEL MEDIATORE: Senza questo, i titoli dei tab rimangono visibili
                        mediator?.detach()
                        mediator = null
                    }

                    // 3. Configurazione Tab dinamica (si attiva se il mediatore è null o appena resettato)
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
                }
        }
    }
    class RidePagerAdapter(fragment: Fragment, private val rideId: String) : FragmentStateAdapter(fragment) {
        var currentItemCount = 2 // Partiamo con Dettagli e Mappa per tutti

        override fun getItemCount(): Int = currentItemCount

        override fun createFragment(position: Int): Fragment {
            val args = Bundle().apply { putString("rideId", rideId) }
            return when (position) {
                0 -> RideDetailFragment().apply { arguments = args }
                1 -> MapFragment().apply { arguments = args }           // Mappa ora è SECONDA
                2 -> ChatFragment().apply { arguments = args }          // Chat ora è TERZA
                3 -> DriverCheckinlFragment().apply { arguments = args } // Check-in è QUARTO
                else -> RideDetailFragment()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}