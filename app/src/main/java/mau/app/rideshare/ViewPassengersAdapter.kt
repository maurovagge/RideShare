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
    // Aggiungiamo questa lista per gestire gli stati (userid + stato)
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

        // Cerchiamo lo stato
        val infoPasseggero = checkinInfo.find { it.userid.equals(user.id, ignoreCase = true) }

        if (infoPasseggero?.stato.equals("CheckIn", ignoreCase = true)) {
            // MOSTRA IL BADGE E RESETTA IL COLORE TESTO NORMALE
            holder.binding.tvBadgeCheckin.visibility = View.VISIBLE
            holder.binding.tvPasseggero.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        } else {
            // NASCONDI IL BADGE
            holder.binding.tvBadgeCheckin.visibility = View.GONE
            holder.binding.tvPasseggero.setTextColor(holder.itemView.context.getColor(android.R.color.black))
        }
    }

    override fun getItemCount() = passengers.size

    // Aggiorna la lista degli utenti (nomi)
    fun updateData(newPassengers: List<User>) {
        this.passengers = newPassengers
        notifyDataSetChanged()
    }

    // NUOVA: Aggiorna solo gli stati del check-in
    fun updateCheckinStatus(newCheckinInfo: List<Passenger>) {
        this.checkinInfo = newCheckinInfo
        notifyDataSetChanged()
    }
}