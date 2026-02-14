package mau.app.rideshare

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class SOSForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sosID: String? = null
    private var locationCallback: LocationCallback? = null

    private var sosContact: String? = null
    val db = com.google.firebase.Firebase.firestore

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // getting current user from database
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        db.collection("Users").document(userId).get().addOnCompleteListener { task ->
            val document = task.result
            if (document != null && document.exists()) {
                sosContact = document.getString("contattoSOS")
                if (sosContact != null) {
                    db.collection("Usernames").document(sosContact!!).get()
                        .addOnCompleteListener { task ->
                            val document = task.result
                            if (document != null && document.exists()) {
                                val sosContactid = document.getString("ownerId")
                                if (sosContactid != null) {
                                    val sos = SOS().apply {
                                        //RideId = currentRide.value!!.id.toString()
                                        SourceUser = document.id
                                        DestinationUser = sosContact!!
                                        State = "ON"
                                        Issued = Timestamp.now()
                                    }
                                    try {
                                        val newSOS = db.collection("SOS").document()
                                        newSOS.set(sos).addOnSuccessListener { docRef ->
                                            Log.d("SOS", "SOS document created ID: ${sos.id}")
                                            sosID = newSOS.id
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SOS", "Error creating SOS document", e)
                                    }
                                }

                            }

                        }

                }
            }
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = createNotification()
        startForeground(1, notification)

        startLocationUpdates()
        return START_STICKY // restart in case of kill
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLocation = locationResult.lastLocation ?: return
                if (sosID == null) return

                val sosLocation: RideShareLocation = RideShareLocation(
                    lastLocation.latitude,
                    lastLocation.longitude
                )

                val docRef = db.collection("SOS").document(sosID.toString())
                docRef.update("soslocation", sosLocation)
                    .addOnSuccessListener { Log.d("SOS", "coordinate aggiornate!") }
                    .addOnFailureListener { e ->
                        Log.w(
                            "SOS",
                            "Errore nell'aggiornamento coordinate",
                            e
                        )
                    }
            }
        }

        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                it,
                Looper.getMainLooper()
            )
        }
    }

    private fun createNotification(): Notification {
        // Intent to open the app from notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // notification builder
        return NotificationCompat.Builder(this, RideShareUtil.LOW_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SOS: Localizzazione Attiva")
            .setContentText("SOS Tracking")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setColor(Color.RED)
            .setColorized(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // closing location updates
        removeLocationUpdates()

        val docRef = db.collection("SOS").document(sosID.toString())
        docRef.update("state", "OFF")
            .addOnSuccessListener { Log.d("SOS", "SOS impostato a OFF") }
            .addOnFailureListener { e -> Log.w("SOS", "Errore nello spegnimento allarme", e) }
    }


    private fun removeLocationUpdates() {
        // removing callback
        fusedLocationClient.removeLocationUpdates(locationCallback!!)
    }

}