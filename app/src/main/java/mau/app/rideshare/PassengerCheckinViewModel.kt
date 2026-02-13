package mau.app.rideshare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PassengerCheckinViewModel : ViewModel() {

    val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    private val db = FirebaseFirestore.getInstance()
    private var _rideId: String? = ""

    // Stato per gestire l'UI (Loading, Successo, Errore)
    val checkInStatus = MutableLiveData<Boolean>()

    fun performCheckIn(rideId: String) {
        if (userId != null) {

            val docRef = db.collection("RideData").document(rideId)

            docRef.get().addOnSuccessListener { document ->
                if (document != null) {

                    val passengers  = document.toObject(Ride::class.java)?.Viaggiatori ?: emptyList()

                    val newPassengers = RideShareUtil.setPassengerOnBoard(userId, passengers)
                    docRef.update("viaggiatori", newPassengers).addOnSuccessListener { checkInStatus.value = true}
                }
            }
        }
    }
}
