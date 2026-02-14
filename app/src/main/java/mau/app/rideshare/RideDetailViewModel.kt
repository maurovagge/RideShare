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
import com.google.firebase.firestore.FieldValue
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

    private val _isUserDriver = MutableStateFlow(false)
    val isUserDriver: StateFlow<Boolean> = _isUserDriver

    private val _isUserJoined = MutableStateFlow(false)
    val isUserJoined: StateFlow<Boolean> = _isUserJoined

    private var rideListener: ListenerRegistration? = null // Listener principale per la Ride
    val isDriver = MutableStateFlow(false)



    fun setRideId(id: String) {
        if (_rideId == id) return
        _rideId = id

    }

    private var isObservingRide = false
    fun observeRide(rideId: String) {
        // if ID is equal to the current id, do nothing
        if (_rideId == rideId && rideListener != null) return

        // reset of last state
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
                _isUserDriver.value = ride?.autista == uid
                _isUserJoined.value = RideShareUtil.isUserIdInPassengers(uid!!, ride?.viaggiatori!!)

                updateDriverListener(ride?.autista)
                updatePassengersListener(ride?.viaggiatori)
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

    // called on viemodel destruction
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

    private fun updatePassengersListener(passengers: List<Passenger>?) {

        if (passengers.isNullOrEmpty()) {
            passengersListener?.remove()
            _passengersListState.value = emptyList()
            return
        }

        val userIdList: List<String> = passengers.map { it.userid }

        passengersListener?.remove()

        //  creating new listener for updated list
        passengersListener = db.collection("Users")
            .whereIn(FieldPath.documentId(), userIdList)
            .addSnapshotListener { querySnapshot, _ ->
                val listaPasseggeri = querySnapshot?.toObjects(User::class.java)
                if (listaPasseggeri != null) {
                    _passengersListState.value = listaPasseggeri
                }
            }
    }

    // joinride function, called when user presses join ride button
    fun joinRide() {

        val uid = currentUserId ?: return
        val rideId = _rideId ?: return

        // updates database
        val docRef = db.collection("RideData").document(rideId)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val passengers  = document.toObject(Ride::class.java)?.viaggiatori ?: emptyList()

                val newPassengers = RideShareUtil.addUserIdToPassengers(uid, passengers)
                docRef.update("viaggiatori", newPassengers)
            }
        }
    }

    //leavenride function, called when user presses leave ride button
    fun leaveRide() {
        val uid = currentUserId ?: return
        val rideId = _rideId ?: return


        // updates database
        val docRef = db.collection("RideData").document(rideId)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {

                val passengers  = document.toObject(Ride::class.java)?.viaggiatori ?: emptyList()

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
            // writing on firebase database
            db.collection("RideData").document(id)
                .update("stato", nextStatus)
                .addOnSuccessListener {
                    if ((nextStatus == "Imbarco") || (nextStatus == "Terminato")) {
                        val passengerIds = RideShareUtil.getUserIdListFromPassengers(currentRide.viaggiatori)

                        val data = hashMapOf(
                            "action" to nextStatus,
                            "timestamp" to FieldValue.serverTimestamp(),
                            "rideid" to id,
                            "passengers" to passengerIds
                        )
                        db.collection("RideActions")
                            .add(data)
                            .addOnSuccessListener { documentReference ->
                                Log.d("Firestore", "Azione $nextStatus creata")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Errore durante la creazione dell'azione di $nextStatus")
                            }
                    }
                }.addOnFailureListener { e ->
                    Log.e("Firestore", "Errore durante il passaggio allo stato $nextStatus")
                }
        }
    }

    // function to signal if driver is changing ride state too early
    fun checkNextStatus(): String {
        val ride = _rideState.value ?: return ""
        val oraAttuale = System.currentTimeMillis()
        val orapartenza = ride.data?.toDate()?.time ?: return ""

        return when (ride.stato) {
            "Disponibile" -> {
                val unOraPrima = orapartenza - 3600000
                if (oraAttuale >= unOraPrima) "Imbarco" else "WarningImbarco"
            }
            "Imbarco" -> {
                if (oraAttuale >= orapartenza) "Iniziato" else "WarningInizio"
            }
            "Iniziato" -> "Terminato"
            else -> "OK"
        }
    }

    fun getNextStatus(): String {
        val ride = _rideState.value ?: return ""
        val oraAttuale = System.currentTimeMillis()
        val orapartenza = ride.data?.toDate()?.time ?: return ""

        return when (ride.stato) {
            "Disponibile" ->  "Imbarco"
            "Imbarco" -> "Iniziato"
            "Iniziato" -> "Terminato"
            else -> ""
        }
    }

    fun getNextStatusLabel(): String {
        val statoAttuale = _rideState.value?.stato ?: return ""
        return when (statoAttuale) {
            "Disponibile" -> "IMBARCO"
            "Imbarco" -> "VIAGGIO INIZIATO"
            "Iniziato" -> "VIAGGIO TERMINATO"
            else -> ""
        }
    }
    fun getNextStatusActionLabel(): String {
        val statoAttuale = _rideState.value?.stato ?: return ""
        return when (statoAttuale) {
            "Disponibile" -> "APRI CHECK-IN"
            "Imbarco" -> "INIZIA VIAGGIO"
            "Iniziato" -> "CONCLUDI VIAGGIO"
            else -> ""
        }
    }


}