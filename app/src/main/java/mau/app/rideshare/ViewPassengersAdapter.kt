import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mau.app.rideshare.User
import mau.app.rideshare.databinding.ViewPassengersItemBinding

class UserAdapter(private val passengers: List<User>) :
    RecyclerView.Adapter<UserAdapter.ViewHolder>() {

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
        holder.binding.tvPasseggero.setText(user.Nome)
    }

    override fun getItemCount() = passengers.size
}