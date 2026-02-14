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
            null
        }
        view.filters = arrayOf(filter)
    }
}


class ProfileFragment : Fragment() {

    //binding object and property
    private var bind: FragmentProfileBinding? = null
    private val binding get() = bind!!

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

        //  Loading data when creating fragment
        profileViewModel.loadUserData()

        profileViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Updates only when data is different from database
                if (binding.etProfileName.text.toString() != it.nome) {
                    binding.etProfileName.setText(it.nome)
                }
                if (binding.etProfileUserTag.text.toString() != it.userTag) {
                    binding.etProfileUserTag.setText(it.userTag)
                }
                if (binding.etProfileEmail.text.toString() != it.email) {
                    binding.etProfileEmail.setText(it.email)
                }
                if (binding.etProfilePhone.text.toString() != it.telefono) {
                    binding.etProfilePhone.setText(it.telefono)
                }
                if (binding.etProfileSOSContact.text.toString() != it.contattoSOS) {
                    binding.etProfileSOSContact.setText(it.contattoSOS)
                }
                if (binding.etProfileSOSSentence.text.toString() != it.fraseSOS) {
                    binding.etProfileSOSSentence.setText(it.fraseSOS)
                }
                if (binding.etProfileCheckInSentence.text.toString() != it.fraseCheckIn) {
                    binding.etProfileCheckInSentence.setText(it.fraseCheckIn)
                }
            }
        }

        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        (requireActivity() as? MainActivity)?.hideOptionMenu()

        binding.buttonProfileSave.setOnClickListener {
            // creating a local instance with the current data
            val userToSave = User(
                id = profileViewModel.currentUser.value?.id ?: "",
                email = binding.etProfileEmail.text.toString().trim(),
                nome = binding.etProfileName.text.toString().trim(),
                userTag = binding.etProfileUserTag.text.toString().trim(),
                telefono = binding.etProfilePhone.text.toString().trim(),
                contattoSOS = binding.etProfileSOSContact.text.toString().trim(),
                fraseSOS = binding.etProfileSOSSentence.text.toString().trim(),
                fraseCheckIn = binding.etProfileCheckInSentence.text.toString().trim(),
                profileSaved = true
            )

            profileViewModel.saveUserProfile(userToSave,
                onSuccess = {
                    Toast.makeText(requireContext(), "Profilo aggiornato!", Toast.LENGTH_SHORT).show()
                    exitFromFragment()
                },
                onError = { error ->
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            )
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