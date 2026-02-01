import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mau.app.rideshare.Message

class ChatViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    // List of messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var chatListener: ListenerRegistration? = null

    // Function to start the listener for messages of a specific ride chat
    fun startChatListener(rideId: String) {
        chatListener?.remove()

        chatListener = db.collection("RideDataTRE").document(rideId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null) {
                    val messageList = snapshot.toObjects(Message::class.java)
                    _messages.value = messageList
                }
            }
    }

    // Funtion to write a message
    fun sendMessage(rideId: String, message: Message) {
        db.collection("RideDataTRE").document(rideId)
            .collection("messages")
            .add(message)
    }

    // Closing of the listener
    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }
}