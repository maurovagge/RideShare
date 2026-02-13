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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DriverCheckinViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var _rideId : String? = ""
    private val _rideState = MutableStateFlow<Ride?>(null)
    val rideState: StateFlow<Ride?> = _rideState

    private val _passengersListState = MutableStateFlow<List<User>>(emptyList())
    val passengersState: StateFlow<List<User>> = _passengersListState

    private var passengersListener: ListenerRegistration? = null
    private var isObservingRide = false
    fun observeRide(rideId: String) {

        if (isObservingRide) return
        isObservingRide = true
        val docRef = db.collection("RideData").document(rideId)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val ride = snapshot.toObject(Ride::class.java)
                _rideState.value = ride

                updatePassengersListener(ride?.checkin)
            }
        }
    }

    private fun updatePassengersListener(passengersIds: List<String>?) {
        if (passengersIds.isNullOrEmpty()) {
            passengersListener?.remove()
            _passengersListState.value = emptyList()
            return
        }
                passengersListener?.remove()

        passengersListener = db.collection("Users")
            .whereIn(FieldPath.documentId(), passengersIds)
            .addSnapshotListener { querySnapshot, _ ->
                val listaPasseggeri = querySnapshot?.toObjects(User::class.java)
                if (listaPasseggeri != null) {
                    _passengersListState.value = listaPasseggeri
                }
            }
    }
}