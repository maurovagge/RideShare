import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.Passenger
import mau.app.rideshare.User
import mau.app.rideshare.databinding.ViewPassengersItemBinding

class UserAdapter(
    private var passengers: List<User>,
    private var checkinInfo: List<Passenger> = emptyList()
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(val binding: ViewPassengersItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewPassengersItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = passengers[position]
        holder.binding.tvPasseggero.text = user.nome // Usa l'ID del tuo XML

        // looks for user state
        val infoPasseggero = checkinInfo.find { it.userid.equals(user.id, ignoreCase = true) }

        if (infoPasseggero?.stato.equals("CheckIn", ignoreCase = true)) {
            // showing checked badge
            holder.binding.tvBadgeCheckin.visibility = View.VISIBLE
            holder.binding.tvPasseggero.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        } else {
            // hiding checked badge
            holder.binding.tvBadgeCheckin.visibility = View.GONE
            holder.binding.tvPasseggero.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }
    }

    override fun getItemCount() = passengers.size

    // updates user list
    fun updateData(newPassengers: List<User>) {
        this.passengers = newPassengers
        notifyDataSetChanged()
    }

    // updates checkin status
    fun updateCheckinStatus(newCheckinInfo: List<Passenger>) {
        this.checkinInfo = newCheckinInfo
        notifyDataSetChanged()
    }
}