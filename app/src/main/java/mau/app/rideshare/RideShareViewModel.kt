package mau.app.rideshare


import android.util.Log
import androidx.databinding.InverseMethod
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.text.SimpleDateFormat
import java.util.Locale

class RideShareViewModel : ViewModel() {

    //main list bound in real time to the recycler view
    private var internalRideList = MutableLiveData<List<Ride>>()

    //current user ride list bound in real time to the recycler view (when filtered only by current user)
    private var internalCurrentUserRideList = MutableLiveData<List<Ride>>()
    //var rideList: LiveData<List<Ride>> = internalRideList

    //list of all users
    private var internalUserList = MutableLiveData<List<User>>()
    val userList: LiveData<List<User>> = internalUserList

    private val onlyMyRides = MutableLiveData(false)

    fun filterMyRides(list:List<Ride>): List<Ride> {
        return list.filter { it.Autista==currentUser.value?.id || it.Viaggiatori.contains(currentUser.value?.id) }
    }

    val rideList = MediatorLiveData<List<Ride>>().apply {
        addSource(internalRideList) { rides ->
            value = if (onlyMyRides.value == true) filterMyRides(rides) else rides
        }
        addSource(onlyMyRides) { isFiltering ->
            value = if (isFiltering) filterMyRides(internalRideList.value!!) else internalRideList.value
        }
    }

    fun changeRideList(bool: Boolean=false){
        onlyMyRides.value=bool
    }


    private val db = Firebase.firestore

    var currentUser = MutableLiveData<User?>()
    var currentRide= MutableLiveData<Ride?>()


    private var internalPassengerList : MutableList<User> = mutableListOf()



    fun getUserFromId(id:String): User?{
        val tmpuser: User? = userList.value?.find { it.id==id }
        return tmpuser
    }

    fun getCurrentRidePassengers(): List<User>
    {

        var myList: MutableList<User> = mutableListOf()
        for (user in userList.value!!)
        {
            if (currentRide.value!!.Viaggiatori.contains(user.id)) {
                myList.add(user)
            }
        }

        internalPassengerList = myList
        return myList
    }

    fun leaveRide(){
        val ride=currentRide.value
        internalPassengerList.removeAll { it.id== currentUser.value?.id }
        currentRide.value!!.Viaggiatori=internalPassengerList.map { it.id as String }
        db.collection("RideDataNew").document(currentRide.value!!.id.toString()).
                update("Viaggiatori", currentRide.value?.Viaggiatori).
                addOnSuccessListener {  currentRide.value=ride  }.addOnFailureListener {  }

    }

    fun deleteRide(){
        val tmpList = internalRideList.value ?: return
        val newList = tmpList.filterNot { it.id == currentRide.value?.id }
        internalRideList.value = newList
        db.collection("RideDataNew").document(currentRide.value!!.id.toString()).delete().
        addOnSuccessListener {  }.addOnFailureListener {  }
    }
    fun joinRide(){
        val ride=currentRide.value
        if(internalPassengerList.none{it.id==currentUser.value?.id}&&(currentUser.value?.id!=currentRide.value!!.Autista)){
            internalPassengerList.add(currentUser.value!!)
            currentRide.value!!.Viaggiatori=internalPassengerList.map { it.id as String }
            db.collection("RideDataNew").document(currentRide.value!!.id.toString()).
            update("Viaggiatori", currentRide.value!!.Viaggiatori).
            addOnSuccessListener { currentRide.value=ride }.addOnFailureListener {  }
        }

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
        val from =  Timestamp.now()

        //get all rides from db
        db.collection("RideDataNew").orderBy("data")
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

        db.collection("UsersNew")
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


    fun saveRide(ride: Ride?) {

        val db = Firebase.firestore
        try {
            db.collection("RideDataNew").add(ride!!)

        } catch (e: Exception) {
            Log.e("RideShareViewModel", "Error saving ride", e)
        }
    }


    fun saveUserProfile(name: String, phone: String) {

        currentUser.value?.Nome = name
        currentUser.value?.Telefono = phone

        val db = Firebase.firestore
        try {
            currentUser.value?.let { user ->
                db.collection("UsersNew").document(currentUser.value?.id!!).set(user)
            }

        } catch (e: Exception) {
            Log.e("RideShareViewModel", "Error saving user profile", e)
        }
    }

    fun getCurrentUserData() {
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val db = Firebase.firestore
        val documentReference =
            db.collection("UsersNew").document(userId).get().addOnCompleteListener { task ->
                val document = task.result
                if (document != null && document.exists()) {
                    currentUser.value = document.toObject(User::class.java)!!
                } else {
                    currentUser.value=User().apply {
                        id = userId
                        Nome = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                        Telefono = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                    }

                    currentUser.value?.let { user ->
                        db.collection("UsersNew").document(userId).set(user)
                    }
                }
            }
    }
}






