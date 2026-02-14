package mau.app.rideshare


import android.util.Log
import androidx.databinding.InverseMethod
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.RoutingParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.type.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class RideShareViewModel : ViewModel() {

    //main list bound in real time to the recycler view
    private var internalRideList = MutableLiveData<List<Ride>>()

    //current user ride list bound in real time to the recycler view (when filtered only by current user)
    private var internalCurrentUserRideList = MutableLiveData<List<Ride>>()

    //list of all users
    private var internalUserList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> = internalUserList

    private val onlyMyRides = MutableLiveData(false)

    private val searchCoords = MutableLiveData(LatLng(0.0, 0.0))

    private val searchDistance = MutableLiveData(100000)

    // function to filter list of rides
    fun filterRides(list: List<Ride>): List<Ride> {
        return list.filter {
            val userIdList: List<String> = it.viaggiatori.map { it.userid }
            (if (onlyMyRides.value == true) (it.autista == currentUser.value?.id || userIdList.contains(
                currentUser.value?.id)) else true) && isInSearchRange(
                it.partenza.AddressCoords.latitude,
                it.partenza.AddressCoords.longitude)

        }
    }


    // Function to search distance from coordinates
    fun isInSearchRange(rideLatitude: Double, rideLongitude: Double) : Boolean
    {
        if (searchCoords.value == null) return true
        if (searchCoords.value == LatLng(0.0, 0.0)) return true

        val distance = GeoFireUtils.getDistanceBetween(
            GeoLocation(rideLatitude, rideLongitude),
            GeoLocation(searchCoords.value!!.latitude, searchCoords.value!!.longitude)
        )
        return distance <= searchDistance.value!!
    }


    // mediator livedata to handle filter functions
    val rideList = MediatorLiveData<List<Ride>>().apply {
        addSource(internalRideList) { rides ->
            value = if (rides != null)
                 filterRides(rides)
            else emptyList()
        }
        addSource(onlyMyRides) { _ ->
            value = if (internalRideList.value != null)
                filterRides(internalRideList.value!!)
            else emptyList()
        }
        addSource(searchCoords) { _ ->
            value = if (internalRideList.value != null)
                filterRides(internalRideList.value!!)
            else emptyList()
        }
        addSource(searchDistance) { _ ->
            value = if (internalRideList.value != null)
                filterRides(internalRideList.value!!)
            else emptyList()
        }
    }



    fun changeRideList(bool: Boolean = false) {
        onlyMyRides.value = bool
    }

    fun changeSearchCoords(latlong : LatLng) {
        searchCoords.value = latlong
    }

    fun changeSearchDistance(distance: Int) {
        searchDistance.value = distance
    }


    private val db = Firebase.firestore

    var currentUser = MutableLiveData<User?>()
    var currentRide = MutableLiveData<Ride?>()


    private var internalPassengerList: MutableList<User> = mutableListOf()

    fun getCurrentRidePassengers(): List<User> {

        var myList: MutableList<User> = mutableListOf()
        for (user in userList.value!!) {
            if (RideShareUtil.isUserIdInPassengers(user.id!!,currentRide.value!!.viaggiatori)) {
                myList.add(user)
            }
        }

        internalPassengerList = myList
        return myList
    }


    fun createSOS(simulate : Boolean) : String {

        var docId = ""
        var destUsername = currentUser.value?.contattoSOS
        if (destUsername == null || destUsername.isEmpty())
        {
            if (simulate) {
                destUsername = currentUser.value?.userTag
            }
            else {
                return ""
            }
        }
        if (destUsername == null || destUsername.isEmpty())
        {
            return ""
        }

        val newSOS = db.collection("SOS").document()
        docId = newSOS.id
        db.collection("Usernames").document(destUsername)
            .get().addOnSuccessListener { docRef ->


                val sos = SOS().apply {
                    if (currentRide.value != null) {
                        RideId = currentRide.value!!.id.toString()
                        SOSLocation = currentRide.value!!.partenza.AddressCoords
                    }
                    else {
                        SOSLocation = RideShareLocation(44.5, 9.0)
                    }
                    SourceUser = currentUser.value!!.id.toString()
                    if (simulate) {
                        DestinationUser = currentUser.value!!.id!!
                        val info = currentUser.value!!.nome + "(@" + currentUser.value!!.userTag + ")"
                        SOSinfo = "Simulazione SOS da $info"
                        Simulation = true
                    }
                    else {
                        DestinationUser = docRef.get("ownerId").toString()
                        val info = currentUser.value!!.nome + "(@" + currentUser.value!!.userTag + ")"
                        SOSinfo = "Richiesta SOS da $info"
                        Simulation = false
                    }
                    State = "ON"
                    Issued = Timestamp.now()
                }
                try {
                    newSOS.set(sos)
                        .addOnSuccessListener { documentReference ->
                            Log.d("SOS", "SOS document created ID: ${newSOS.id}")
                        }
                } catch (e: Exception) {
                    Log.e("RideShareViewModel", "Error saving SOS", e)
                }
            }

        return docId
    }


    fun convertDateStringToTimestamp(dataora: String): Timestamp? {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return try {
            val data = format.parse(dataora)
            if (data != null) Timestamp(data) else null
        } catch (e: Exception) {
            Log.e("RideShareViewModel", "Error converting date string to Timestamp", e)
            null
        }
    }


    init {
        val db = Firebase.firestore
        getCurrentUserData()

        //exclude rides in the past
        val from = Timestamp.now()

        //get all rides from db
        db.collection("RideData").orderBy("data")
            .whereGreaterThanOrEqualTo("data", from)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rides = snapshot.toObjects(Ride::class.java)
                    internalRideList.value = rides
                }
            }


        db.collection("Users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Gestisci l'errore (es. log o messaggio all'utente)
                    return@addSnapshotListener
                }


                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    internalUserList.value = users
                }
            }

        updateFcmToken()
    }

    fun getUserById(id: String): User? {
        if (userList.value == null) return null

        for (user in userList.value!!) {
            if (user.id == id) {
                return user
            }
        }
        return null
    }

    val tempoStimato = MutableLiveData<Long>()

    val context = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_API_KEY).build()

    // function to calculate the expected duration of a ride
    fun CalculateRideRouteTime(partenza: RideShareLocation, arrivo: RideShareLocation) {

        val originApi = com.google.maps.model.LatLng(partenza.latitude, partenza.longitude)
        val destinationApi = com.google.maps.model.LatLng(arrivo.latitude, arrivo.longitude)

        viewModelScope.launch(Dispatchers.IO) {
            val request = DirectionsApi.newRequest(context)
                .origin(originApi)
                .destination(destinationApi)
                .mode(com.google.maps.model.TravelMode.DRIVING)
                .await() // Questa funzione sospende la coroutine senza bloccare la UI

            val durata = request.routes[0].legs[0].duration

            withContext(Dispatchers.Main) {
                tempoStimato.value = durata.inSeconds
            }
        }
    }

    fun saveRide(ride: Ride?) {

        if (ride != null) {
            val db = Firebase.firestore
            try {
                db.collection("RideData").add(ride!!)

            } catch (e: Exception) {
                Log.e("RideShareViewModel", "Error saving ride", e)
            }
        }
    }


    fun saveUserProfile(user: User) {

        currentUser.value?.nome = user.nome
        currentUser.value?.telefono = user.telefono
        currentUser.value?.contattoSOS = user.contattoSOS
        currentUser.value?.fraseSOS = user.fraseSOS
        currentUser.value?.fraseCheckIn = user.fraseCheckIn

        val db = Firebase.firestore
        try {
            currentUser.value?.let { user ->
                db.collection("Users").document(currentUser.value?.id!!).set(user)
            }

        } catch (e: Exception) {
            Log.e("RideShareViewModel", "Error saving user profile", e)
        }
    }

    fun updateFcmToken() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener

            val token = task.result
            val userRef = Firebase.firestore.collection("Users").document(currentUser.uid)

            // Salva o aggiorna il token nel documento dell'utente
            userRef.update("fcmToken", token)
                .addOnFailureListener {
                    // Se il documento non esiste, crealo
                    userRef.set(hashMapOf("fcmToken" to token), SetOptions.merge())
                }
        }
    }

    fun getCurrentUserData() {
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val db = Firebase.firestore
        val documentReference =
            db.collection("Users").document(userId).get().addOnCompleteListener { task ->
                val document = task.result
                if (document != null && document.exists()) {
                    currentUser.value = document.toObject(User::class.java)!!
                } else {
                    currentUser.value = User().apply {
                        id = userId
                        nome = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                        telefono = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                    }

                    currentUser.value?.let { user ->
                        db.collection("Users").document(userId).set(user)
                    }
                }
            }
    }
}








