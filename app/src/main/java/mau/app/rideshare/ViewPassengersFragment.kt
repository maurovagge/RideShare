package mau.app.rideshare

import UserAdapter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentViewPassengersBinding
import kotlin.getValue


class ViewPassengersFragment : Fragment() {


    private var bind: FragmentViewPassengersBinding? = null
    private val binding get() = bind!!
    private val sharedViewModel: RideShareViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentViewPassengersBinding.inflate(inflater, container, false)
        binding.viewModel = sharedViewModel
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        (requireActivity() as? MainActivity)?.hideOptionMenu()

        val adapter = UserAdapter(sharedViewModel.getCurrentRidePassengers())
        binding.rvNomi.adapter = adapter
    }
}