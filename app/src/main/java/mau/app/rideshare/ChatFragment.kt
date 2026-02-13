package mau.app.rideshare

import ChatAdapter
import ChatViewModel
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mau.app.rideshare.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private val sharedViewModel: RideShareViewModel by activityViewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = sharedViewModel.currentUser.value
        val currentRide = sharedViewModel.currentRide.value

        if (currentUser == null || currentRide == null) return

        // Initializing adapter
        chatAdapter = ChatAdapter(currentUser.id!!)
        binding.rvChat.adapter = chatAdapter


        chatViewModel.startChatListener(currentRide.id!!)

        // Watching stateFlow, updating the list for new messages
        viewLifecycleOwner.lifecycleScope.launch {
            chatViewModel.messages.collect { messaggesList ->
                chatAdapter.submitList(messaggesList) {
                    // Automatic scroll to last message when new message come
                    if (messaggesList.isNotEmpty()) {
                        binding.rvChat.scrollToPosition(messaggesList.size - 1)
                    }
                }
            }
        }

        // SEND MESSAGE button
        binding.btnInvia.setOnClickListener {
            val testo = binding.etMessaggio.text.toString().trim()
            if (testo.isNotEmpty()) {
                val newMessage = Message(
                    senderId = currentUser.id!!,
                    senderName = sharedViewModel.currentUser.value?.nome,
                    text = testo,
                    timestamp = System.currentTimeMillis()
                )
                chatViewModel.sendMessage(currentRide.id, newMessage)
                binding.etMessaggio.text.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}