package mau.app.rideshare

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
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

class RideMonitorService : Service(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var sosID: String? = null
    private var locationCallback: LocationCallback? = null

    private var sosContact: String? = null
    val db = com.google.firebase.Firebase.firestore


    private var isSOSActive = false
    private var isListening = false

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private fun toggleAccelerometer(enable: Boolean) {
        if (enable) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager.unregisterListener(this)
        }
    }

    private fun startListening() {
        if (!isListening) {
            isListening = true
            // Logica di avvio Vosk...
            //updateNotification("In ascolto per comando vocale...")
        }
    }

    private fun startSOS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        db.collection("Users").document(userId).get().addOnCompleteListener { task ->
            val document = task.result
            if (document != null && document.exists()) {
                sosContact = document.getString("contattoSOS")
            }
            if (sosContact != null) {
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
        startLocationUpdates()
        isSOSActive = true
    }

    private fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    lateinit var wakeLock: PowerManager.WakeLock
    override fun onCreate() {
        super.onCreate()

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RideShare::ShakeWakeLock").apply {
                    acquire()
                }
            }

        // Recuperi il servizio dal sistema
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Ottieni il sensore specifico (l'accelerometro)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            if (intent.action == "ACTION_STOP_SOS") {
                SwitchSOSOff()
            }
            else {
                val notification = createNotification()
                startForeground(1, notification)

                toggleAccelerometer(true)
            }
        }
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
        // 1. Intent per riaprire l'app se l'utente clicca sulla notifica
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE // Obbligatorio per Android 12+
        )

        // 2. Costruzione della notifica
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

    fun stopSOS() {

        if (isSOSActive) {
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback!!)
            }
        }
        isSOSActive = false
    }

    fun stopListening() {

        if (isListening) {
          //stop vosk
        }
        isListening = false
    }

    fun SwitchSOSOff () {
        stopSOS()
        stopListening()

        val docRef = db.collection("SOS").document(sosID.toString())
        docRef.update("state", "OFF")
            .addOnSuccessListener { Log.d("SOS", "SOS impostato a OFF") }
            .addOnFailureListener { e -> Log.w("SOS", "Errore nello spegnimento allarme", e) }
        stopSelf()
    }


    override fun onDestroy() {
        super.onDestroy()

        stopSOS()
        stopListening()
        toggleAccelerometer(false)
        wakeLock.release()

    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("Not yet implemented")
    }

    var shakeThreshold = 7.0f
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calcoliamo l'accelerazione totale (G-Force) sottraendo la gravità
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH
            Log.d("RIDE MONITOR", "Accelerazione totale: $gForce")


            if (gForce > shakeThreshold) {
                if (isListening)
                {
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, già in ascolto vocale")
                }
                else if (isSOSActive)
                {
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, SOS già attivo")
                }
                else
                {
                    startListening()
                    startSOS()
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, già in ascolto vocale")
                }
            }
        }
    }

}