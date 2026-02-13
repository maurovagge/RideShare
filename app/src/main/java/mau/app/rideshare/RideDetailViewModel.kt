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
    private var _rideId: String? = ""
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

    private var rideListener: ListenerRegistration? = null // Listener principale per la Ride
    val isDriver = MutableStateFlow(true)



    fun setRideId(id: String) {
        if (_rideId == id) return
        _rideId = id

    }

    private var isObservingRide = false
    fun observeRide(rideId: String) {
        // Rimuoviamo il blocco "if (isObservingRide) return"
        // Se l'ID è lo stesso di quello attuale, non fare nulla per evitare loop
        if (_rideId == rideId && rideListener != null) return

        // 1. RESET TOTALE dello stato precedente
        clearAllListeners()
        resetState()

        this._rideId = rideId

        val docRef = db.collection("RideData").document(rideId)

        rideListener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val ride = snapshot.toObject(Ride::class.java)
                _rideState.value = ride

                val uid = currentUserId
                _isUserDriver.value = ride?.Autista == uid
                _isUserJoined.value = RideShareUtil.isUserIdInPassengers(uid!!, ride?.Viaggiatori!!)

                updateDriverListener(ride?.Autista)
                updatePassengersListener(ride?.Viaggiatori)
            }
        }
    }

    private fun resetState() {
        _rideState.value = null
        _driverState.value = null
        _passengersListState.value = emptyList()
        _isUserDriver.value = false
        _isUserJoined.value = false
    }

    private fun clearAllListeners() {
        rideListener?.remove()
        driverListener?.remove()
        passengersListener?.remove()
    }

    // Chiamato quando il ViewModel viene distrutto
    override fun onCleared() {
        super.onCleared()
        clearAllListeners()
    }

    private fun updateDriverListener(autistaId: String?) {

        if (autistaId == null) {
            driverListener?.remove()
            _driverState.value = null
            return
        }

        driverListener?.remove()

        driverListener = db.collection("Users").document(autistaId)
            .addSnapshotListener { userDoc, _ ->
                if (userDoc != null && userDoc.exists()) {
                    _driverState.value = userDoc.toObject(User::class.java)
                }
            }
    }

    // --- FUNZIONE PER I PASSEGGERI ---
    private fun updatePassengersListenerOLD(passengersIds: List<String>?) {
        // 1. Se la lista è vuota, chiudiamo il listener e puliamo la lista
        if (passengersIds.isNullOrEmpty()) {
            passengersListener?.remove()
            _passengersListState.value = emptyList()
            return
        }

        // 2. Rimuoviamo il vecchio listener
        passengersListener?.remove()

        // 3. Creiamo il nuovo listener per la lista aggiornata
        passengersListener = db.collection("Users")
            .whereIn(FieldPath.documentId(), passengersIds)
            .addSnapshotListener { querySnapshot, _ ->
                val listaPasseggeri = querySnapshot?.toObjects(User::class.java)
                if (listaPasseggeri != null) {
                    _passengersListState.value = listaPasseggeri
                }
            }
    }

    private fun updatePassengersListener(passengers: List<Passenger>?) {

        if (passengers.isNullOrEmpty()) {
            passengersListener?.remove()
            _passengersListState.value = emptyList()
            return
        }

        val userIdList: List<String> = passengers.map { it.Userid }

        passengersListener?.remove()

        // 3. Creiamo il nuovo listener per la lista aggiornata
        passengersListener = db.collection("Users")
            .whereIn(FieldPath.documentId(), userIdList)
            .addSnapshotListener { querySnapshot, _ ->
                val listaPasseggeri = querySnapshot?.toObjects(User::class.java)
                if (listaPasseggeri != null) {
                    _passengersListState.value = listaPasseggeri
                }
            }
    }

    fun joinRide() {

        val uid = currentUserId ?: return
        val rideId = _rideId ?: return

        val docRef = db.collection("RideData").document(rideId)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val passengers =
                    document.get("viaggiatori") as? List<Passenger> ?: return@addOnSuccessListener

                val newPassengers = RideShareUtil.addUserIdToPassengers(uid, passengers)
                docRef.update("viaggiatori", newPassengers)
            }
        }
    }

    fun leaveRide() {
        val uid = currentUserId ?: return
        val rideId = _rideId ?: return

        val docRef = db.collection("RideData").document(rideId)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {

                val passengers  = document.toObject(Ride::class.java)?.Viaggiatori ?: emptyList()

                val newPassengers = RideShareUtil.removeUserIdFromPassengers(uid, passengers)
                docRef.update("viaggiatori", newPassengers)
            }
        }
    }

    fun deleteRide() {
        val rideId = _rideId ?: return

        db.collection("RideData").document(rideId).delete()
    }

    fun moveToNextStatus() {
        val id = _rideId ?: return
        val currentRide = _rideState.value ?: return
        val nextStatus = getNextStatus()

        if (nextStatus.isNotEmpty()) {
            // DEVI SCRIVERE SU FIRESTORE
            db.collection("RideData").document(id)
                .update("Stato", nextStatus)
                .addOnSuccessListener {
                    // Ora Firestore manderà un segnale a tutti i telefoni
                    // e la UI si aggiornerà da sola tramite il listener
                }
        }
    }

    fun getNextStatus(): String {
        val ride = _rideState.value ?: return ""
        val oraAttuale = System.currentTimeMillis()
        val oraPartenza = ride.Data?.toDate()?.time ?: return ""

        return when (ride.Stato) {
            "Disponibile" -> {
                // Un'ora prima della partenza (3600000 millisecondi)
                val unOraPrima = oraPartenza - 3600000
                if (oraAttuale >= unOraPrima) "Imbarco" else "TroppoPrestoImbarco"
            }

            "Imbarco" -> {
                // Non si può iniziare finché non è l'orario esatto
                if (oraAttuale >= oraPartenza) "Iniziato" else "TroppoPrestoInizio"
            }

            "Iniziato" -> "Terminato"
            else -> ""
        }
    }

    fun getNextStatusLabel(): String {
        val statoAttuale = _rideState.value?.Stato ?: return ""
        return when (statoAttuale) {
            "Disponibile" -> "IMBARCO"
            "Imbarco" -> "VIAGGIO INIZIATO"
            "Iniziato" -> "VIAGGIO TERMINATO"
            else -> ""
        }
    }

}