import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.Message
import mau.app.rideshare.R
import mau.app.rideshare.RideShareViewModel
import kotlin.getValue




class ChatAdapter(private val currentUserId: String) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2


    // Determine the layout to use depending on the sender
    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_sent_item, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.chat_received_item, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentViewHolder) {
            holder.bind(message)
        } else if (holder is ReceivedViewHolder) {
            holder.bind(message)
        }
    }

    // ViewHolder for your messages (on the right side of the screen)
    inner class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageSent)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestampSent)
        fun bind(message: Message) {
            tvMessage.text = message.text
            tvTimestamp.text = formatTime(message.timestamp)
        }
    }

    // ViewHolder for other's messages (on the left side of the screen)
    inner class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessageReceived)
        private val tvName: TextView = itemView.findViewById(R.id.tvSenderName)

        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestampReceived)
        fun bind(message: Message) {
            tvMessage.text = message.text
            tvName.text = message.senderName
            tvTimestamp.text = formatTime(message.timestamp)
        }
    }
}

// Logic to calculate differences between messages
class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem.timestamp == newItem.timestamp && oldItem.senderId == newItem.senderId

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean =
        oldItem == newItem
}

// convert Timestamp in "HH::mm"
private fun formatTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}