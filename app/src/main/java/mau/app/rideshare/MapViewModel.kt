package mau.app.rideshare


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class MapViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _rideState = MutableStateFlow<Ride?>(null)
    val rideState: StateFlow<Ride?> = _rideState

    private val _sosState = MutableStateFlow<SOS?>(null)
    val sosState: StateFlow<SOS?> = _sosState

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    var listaPunti: List<LatLng> = listOf();

    private var isObservingRide = false

    private var isObservingSOS = false

    private var isObservingAnySOS = false
    fun observeRide(rideId: String) {

        if (isObservingRide) return
        isObservingRide = true
        val docRef = db.collection("RideData").document(rideId)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val ride = snapshot.toObject(Ride::class.java)
                _rideState.value = ride
                if (ride != null) {
                    CalculateRideRoute(ride!!.Partenza.AddressCoords, ride!!.Arrivo.AddressCoords)
                }

            }
        }
    }

    fun observeSOS(sosId: String) {

        if (isObservingSOS) return
        isObservingSOS = true

        val docRef = db.collection("SOS").document(sosId)

        docRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val sos = snapshot.toObject(SOS::class.java)
                _sosState.value = sos
            }
        }
    }

    fun observeAnySOSforMe() {

        if (isObservingAnySOS) return
        isObservingAnySOS = true

        var myUserId = FirebaseAuth.getInstance().currentUser?.uid

        db.collection("SOS")
            .whereEqualTo("destinationUser", myUserId)
            .whereEqualTo("state", "ON")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    if (snapshot.documents.count() > 0) {

                        val firstDoc = snapshot.documents[0]
                        val sos = firstDoc.toObject(SOS::class.java)
                        _sosState.value = sos
                    }
                }
            }
    }


    val context = GeoApiContext.Builder().apiKey(BuildConfig.MAPS_API_KEY).build()
    fun CalculateRideRoute(partenza: RideShareLocation, arrivo: RideShareLocation) {

        val originApi = com.google.maps.model.LatLng(partenza.latitude, partenza.longitude)
        val destinationApi = com.google.maps.model.LatLng(arrivo.latitude, arrivo.longitude)

        viewModelScope.launch(Dispatchers.IO) {
            val request = DirectionsApi.newRequest(context)
                .origin(originApi)
                .destination(destinationApi)
                .mode(com.google.maps.model.TravelMode.DRIVING)
                .await() // Questa funzione sospende la coroutine senza bloccare la UI

            //val durata = request.routes[0].legs[0]duration

            _routePoints.value = PolyUtil.decode(request.routes[0].overviewPolyline.encodedPath)
        }
    }
}


