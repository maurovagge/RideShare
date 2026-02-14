package mau.app.rideshare

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController

import mau.app.rideshare.databinding.FragmentAuthBinding
import mau.app.rideshare.databinding.FragmentMapBinding
import kotlin.getValue

class AuthFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        binding.viewModel = authViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Inserisci email e password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (authViewModel.isRegisterMode.value == true) {

                val nome = binding.etNome.text.toString().trim()
                val userTag = binding.etUsername.text.toString().trim()
                val telefono = binding.etTelefono.text.toString().trim()

                // local validation (no firebase call)
                if (nome.isEmpty() || userTag.isEmpty() || telefono.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Nome, Username e Telefono sono obbligatori",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                authViewModel.checkUsernameAvailability( userTag, onSuccess = {
                    authViewModel.register(
                        email = email,
                        pass = password,
                        nome = nome,
                        userTag = userTag,
                        telefono = telefono,
                        onSuccess = {
                            // Starting MainActivity
                            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            requireActivity().finish()
                        },
                        onError = { messaggioErrore ->
                            Toast.makeText(requireContext(), messaggioErrore, Toast.LENGTH_LONG).show()
                        }
                    )
                }, onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                })

            } else {
                // Login logic
                authViewModel.login(
                    email, password,
                    onSuccess = {
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}