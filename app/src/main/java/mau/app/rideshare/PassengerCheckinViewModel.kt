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

    val userId : String? = FirebaseAuth.getInstance().currentUser?.uid

    private val db = FirebaseFirestore.getInstance()
    private var _rideId : String? = ""
    // Stato per gestire l'UI (Loading, Successo, Errore)
    val checkInStatus = MutableLiveData<CheckInResult>()

    fun performCheckIn(rideId: String) {
        if (userId != null) {
            val rideRef = db.collection("RideDataTRE").document(rideId)

            rideRef.update("Checkin", FieldValue.arrayUnion(userId)).addOnSuccessListener {
            }.addOnSuccessListener {
                checkInStatus.value = CheckInResult.Success
            }.addOnFailureListener { e ->
                checkInStatus.value = CheckInResult.Error(e.message ?: "Errore sconosciuto")
            }
        }
    }
}

sealed class CheckInResult {
    object Success : CheckInResult()
    data class Error(val message: String) : CheckInResult()
}