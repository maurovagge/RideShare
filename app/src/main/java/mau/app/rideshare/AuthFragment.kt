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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AuthFragment.newInstance] factory method to
 * create an instance of this fragment.
 */






class AuthFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        binding.viewModel = authViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Inserisci email e password", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            if (authViewModel.isRegisterMode.value == true) {
                // --- LOGICA REGISTRAZIONE ---
                // 1. Recuperiamo i nuovi dati obbligatori
                val nome = binding.etNome.text.toString().trim()
                val userTag = binding.etUsername.text.toString().trim()
                val telefono = binding.etTelefono.text.toString().trim()

                // 2. Validazione locale (evitiamo chiamate inutili a Firebase)
                if (nome.isEmpty() || userTag.isEmpty() || telefono.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Nome, Username e Telefono sono obbligatori",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // 3. Chiamata alla nuova funzione register con i 5 parametri + callback
                authViewModel.register(
                    email = email,
                    pass = password,
                    nome = nome,
                    userTag = userTag,
                    telefono = telefono,
                    onSuccess = {
                        // Se tutto va bene, andiamo in MainActivity (niente più utenti fantasma!)
                        val intent = Intent(requireContext(), MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
                    },
                    onError = { messaggioErrore ->
                        // Gestione errore (es. email già usata o errore database)
                        Toast.makeText(requireContext(), messaggioErrore, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                // --- LOGICA LOGIN ---
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