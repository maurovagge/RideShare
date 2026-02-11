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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RideDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var _rideId : String? = ""
    private val _rideState = MutableStateFlow<Ride?>(null)
    val rideState: StateFlow<Ride?> = _rideState
    val isDriver = MutableStateFlow(true)
    val departureLocation = MutableStateFlow("Milano, Stazione Centrale")
    val arrivalLocation = MutableStateFlow("Torino, Porta Nuova")


    fun setRideId(id: String) {
        if (_rideId == id) return
        _rideId = id

    }

    private var isObservingRide = false
    fun observeRide(rideId: String) {

        if (isObservingRide) return
        isObservingRide = true
        val docRef = db.collection("RideDataTRE").document(rideId)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val ride = snapshot.toObject(Ride::class.java)
                _rideState.value = ride
            }
        }
    }

    fun moveToNextStatus() {
        _rideState.value!!.Stato = getNextStatus()
    }

    fun getNextStatus() : String {
        if (_rideState.value!!.Stato == "Disponibile")
            return "Imbarco"
        else if (_rideState.value!!.Stato == "Imbarco")
            return "Iniziato"
        else if (_rideState.value!!.Stato == "Iniziato")
            return "Terminato"
        else
            return ""
    }


}