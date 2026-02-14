package mau.app.rideshare

import android.graphics.Color
import android.view.LayoutInflater
import androidx.recyclerview.widget.ListAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.databinding.ListItemBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


//custom bind function, to bind directly the recycler view to the list in the vciew model
@BindingAdapter("items")
fun setRecyclerViewItems(recyclerView: RecyclerView, items: List<Ride>?) {
    val adapter = recyclerView.adapter as? ListBindingAdapter
    adapter?.submitList(items ?: emptyList())
}

//custom bind for date time formatting
@BindingAdapter("dateFromTimestamp")
fun setDateFromTimestamp(view: TextView, timestamp: com.google.firebase.Timestamp ?) {
    if (timestamp != null ) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = Date(timestamp.seconds * 1000)
        view.text = sdf.format(date)
    } else {
        view.text = ""
    }
}

@BindingAdapter("statusColor")
fun setStatusColor(view: TextView, status: String?) {
    val colorHex = when (status) {
        "Disponibile" -> "#2ECC71"
        "Imbarco" -> "#F1C40F"
        "Partito" -> "#3498DB"
        "Arrivato" -> "#27AE60"
        "Annullato" -> "#E74C3C"
        else -> "#000000"
    }
    try {
        view.setTextColor(Color.BLACK)
        view.setTextColor(Color.parseColor(colorHex))
    } catch (e: IllegalArgumentException) {
               view.setTextColor(Color.BLACK)
    }
}

//Binding adapter for the recycler view
class ListBindingAdapter (private val onItemClick: (Ride) -> Unit) :
    ListAdapter<Ride, ListBindingAdapter.RideViewHolder>(RideDiffCallback())
{
    class RideViewHolder(private val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Ride, clickListener: (Ride) -> Unit) {
            binding.ride = item
            binding.root.setOnClickListener { clickListener(item) }
            binding.executePendingBindings()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RideViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding : ListItemBinding = ListItemBinding.inflate(inflater, parent, false)
        return RideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RideViewHolder, position: Int) {

        val ride = getItem(position)
        holder.bind(ride, onItemClick)
    }
    class RideDiffCallback : DiffUtil.ItemCallback<Ride>() {
        override fun areItemsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ride, newItem: Ride): Boolean {
            return oldItem == newItem
        }
    }

}