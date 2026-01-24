package mau.app.rideshare

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentListBinding
import mau.app.rideshare.databinding.FragmentProfileBinding
import kotlin.getValue

class ProfileFragment : Fragment() {

    //binding object and property
    private var bind: FragmentProfileBinding? = null
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
        bind = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = sharedViewModel
        binding.lifecycleOwner=viewLifecycleOwner
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)

        }

        (requireActivity() as? MainActivity)?.hideOptionMenu()

        val button: Button = view.findViewById<Button>(R.id.buttonProfileSave)

        //save the profile to the database (by view model)
        button.setOnClickListener {
            val user = User()
            user.Nome = binding.etProfileName.text.toString()
            user.Telefono  = binding.etProfilePhone.text.toString()

            sharedViewModel.saveUserProfile(user.Nome, user.Telefono)

            //move back to main fragment
            val navController = findNavController()
            navController.navigate(R.id.action_profileFragment_to_listFragment)
        }
    }
}