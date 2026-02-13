package mau.app.rideshare

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import mau.app.rideshare.databinding.FragmentDetailBinding
import mau.app.rideshare.databinding.FragmentListBinding
import mau.app.rideshare.databinding.FragmentProfileBinding
import kotlin.getValue

@BindingAdapter("userTagAllowed")
fun setAllowedChars(view: EditText, enabled: Boolean) {
    if (enabled) {
        val filter = InputFilter { source, start, end, _, _, _ ->
            for (i in start until end) {
                if (!Character.isLetterOrDigit(source[i])) {
                    return@InputFilter ""
                }
            }
            null // Accetta il carattere
        }
        view.filters = arrayOf(filter)
    }
}


class ProfileFragment : Fragment() {

    //binding object and property
    private var bind: FragmentProfileBinding? = null
    private val binding get() = bind!!

    //the view model
    //private val sharedViewModel: RideShareViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        bind = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = profileViewModel
        binding.lifecycleOwner = viewLifecycleOwner
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
            user.UserTag = binding.etProfileUserTag.text.toString()
            user.Email = binding.etProfileEmail.text.toString()
            user.Telefono = binding.etProfilePhone.text.toString()
            user.ContattoSOS = binding.etProfileSOSContact.text.toString()
            user.FraseSOS = binding.etProfileSOSSentence.text.toString()
            user.FraseCheckIn = binding.etProfileCheckInSentence.text.toString()
            user.ProfileSaved = profileViewModel.currentUser?.value?.ProfileSaved ?: false

            profileViewModel.saveUserProfile(
                user,
                onSuccess = {
                    exitFromFragment()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            )

            //move back to main fragment

        }
    }
    private fun exitFromFragment()
    {
        val navController = findNavController()

        if (navController.currentDestination?.getAction(R.id.action_profileFragment_to_listFragment) != null) {
            navController.navigate(R.id.action_profileFragment_to_listFragment)
        } else {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finish()
        }
    }
}