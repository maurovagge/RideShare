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

class RideDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var _rideId : String? = ""
    private val _rideState = MutableStateFlow<Ride?>(null)
    private val _driverState = MutableStateFlow<User?>(null)

    private val _passengersListState = MutableStateFlow<List<User>>(emptyList())
    val passengersState: StateFlow<List<User>> = _passengersListState
    val rideState: StateFlow<Ride?> = _rideState
    val driverState: StateFlow<User?> = _driverState

    private var driverListener: ListenerRegistration? = null
    private var passengersListener: ListenerRegistration? = null

    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId: String? = auth.currentUser?.uid

    // Stato per capire se l'utente è l'autista o un passeggero
    private val _isUserDriver = MutableStateFlow(false)
    val isUserDriver: StateFlow<Boolean> = _isUserDriver

    private val _isUserJoined = MutableStateFlow(false)
    val isUserJoined: StateFlow<Boolean> = _isUserJoined
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

        this._rideId = rideId

        val docRef = db.collection("RideDataTRE").document(rideId)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val ride = snapshot.toObject(Ride::class.java)
                _rideState.value = ride
                val uid = currentUserId
                _isUserDriver.value = ride?.Autista == uid
                _isUserJoined.value = ride?.Viaggiatori?.contains(uid) == true

                // --- 1. Listener per l'Autista ---
                ride?.Autista?.let { autistaId ->
                    // Rimuoviamo il vecchio listener se l'ID autista cambia
                    driverListener?.remove()

                    driverListener = db.collection("UsersTre").document(autistaId)
                        .addSnapshotListener { userDoc, _ ->
                            val driver = userDoc?.toObject(User::class.java)
                            _driverState.value = driver // Aggiorna il tuo StateFlow/LiveData
                        }
                }

                // --- 2. Listener per i Passeggeri ---
                val passengersIds = ride?.Viaggiatori
                if (!passengersIds.isNullOrEmpty()) {
                    // Rimuoviamo il vecchio listener prima di crearne uno nuovo sulla lista aggiornata
                    passengersListener?.remove()

                    passengersListener = db.collection("UsersTre")
                        .whereIn(FieldPath.documentId(), passengersIds)
                        .addSnapshotListener { querySnapshot, _ ->
                            val listaPasseggeri = querySnapshot?.toObjects(User::class.java)
                            if (listaPasseggeri != null) {
                                _passengersListState.value = listaPasseggeri
                            }
                        }
                }
            }
        }
    }

    fun joinRide() {
        val uid = currentUserId ?: return
        val rideId = _rideId ?: return

        // Aggiorna l'array "Viaggiatori" su Firestore
        db.collection("RideDataTRE").document(rideId)
            .update("Viaggiatori", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
    }

    fun leaveRide() {
        val uid = currentUserId ?: return
        val rideId = _rideId ?: return

        db.collection("RideDataTRE").document(rideId)
            .update("Viaggiatori", com.google.firebase.firestore.FieldValue.arrayRemove(uid))
    }

    fun deleteRide() {
        val rideId = _rideId ?: return

        db.collection("RideDataTRE").document(rideId).delete()
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