package mau.app.rideshare

import VoiceCommandManager
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
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.TextView
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

    private lateinit var voiceManager: VoiceCommandManager
    private var sosID: String? = null
    private var locationCallback: LocationCallback? = null

    private var sosContact: String? = null
    val db = com.google.firebase.Firebase.firestore

    private var keywordON = ""
    private var keywordOFF = ""
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

            val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            db.collection("Users").document(userId).get().addOnCompleteListener { task ->
                val document = task.result
                if (document != null && document.exists()) {
                    keywordON = document.getString("fraseSOS").toString()
                    keywordOFF = document.getString("fraseCheckIn").toString()

                    voiceManager.start(keywordON, keywordOFF)
                }
            }
        }
    }


    private fun setupVosk() {

        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        db.collection("Users").document(userId).get().addOnCompleteListener { task ->
            val document = task.result
            if (document != null && document.exists()) {
                keywordON = document.getString("fraseSOS").toString()
                keywordOFF = document.getString("fraseCheckIn").toString()

                voiceManager.setup(keywordON, keywordOFF)
            }
        }
    }


    private fun startSOS() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
                                        DestinationUser = sosContactid
                                        State = "ON"
                                        Issued = Timestamp.now()
                                    }
                                    try {
                                        db.collection("SOS").add(sos)
                                            .addOnSuccessListener { docRef ->
                                                Log.d(
                                                    "SOS",
                                                    "SOS document created ID: ${docRef.id}"
                                                )
                                                sosID = docRef.id
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

        voiceManager = VoiceCommandManager(this)
        voiceManager.onCommandDetected = { comando ->

            Log.d("Vosk", "Rilevato: $comando")
            if (comando.equals(keywordON, ignoreCase = true)) {
                vibratePhone(this, 250)
                startSOS()
            } else if (comando.equals(keywordOFF, ignoreCase = true)) {
                stopSOS()
                stopListening()
                vibratePhone(this, 500)
            }
        }

        setupVosk()


        // Recuperi il servizio dal sistema
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Ottieni il sensore specifico (l'accelerometro)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        var done: Boolean = false
        if (intent != null) {
            if (intent.action != null) {
                if (intent.action!!.equals("ACTION_STOP_SOS", ignoreCase = true)) {
                    done = true
                    SwitchSOSOff()
                }
                if (intent.action!!.equals("Imbarco", ignoreCase = true)) {
                    done = true
                    StopRideMonitor()
                }
            }
            if (done == false) {
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE // Obbligatorio per Android 12+
        )

        return NotificationCompat.Builder(this, RideShareUtil.LOW_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Ride Share")
            .setContentText("Ride Monitor Attivo")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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


        if (sosID != null) {
            if (sosID!!.isNotEmpty()) {
                db.collection("SOS").document(sosID!!)
                    .update("state", "OFF")
                    .addOnSuccessListener {
                        Log.d("SOS", "SOS switched off for ID: ${sosID!!}")
                    }
                isSOSActive = false
            }
        }
    }

    fun stopListening() {

        if (isListening) {
            voiceManager.stop()
        }
        isListening = false
    }

    fun SwitchSOSOff() {
        stopSOS()
        stopListening()

        val docRef = db.collection("SOS").document(sosID.toString())
        docRef.update("state", "OFF")
            .addOnSuccessListener { Log.d("SOS", "SOS impostato a OFF") }
            .addOnFailureListener { e -> Log.w("SOS", "Errore nello spegnimento allarme", e) }
        //stopSelf()
    }

    fun StopRideMonitor() {
        SwitchSOSOff()
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
    }

    var shakeThreshold = 7.0f
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calcoliamo l'accelerazione totale (G-Force) sottraendo la gravità
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble())
                .toFloat() / SensorManager.GRAVITY_EARTH
            //Log.d("RIDE MONITOR", "Accelerazione totale: $gForce")


            if (gForce > shakeThreshold) {
                if (isListening) {
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, già in ascolto vocale")
                } else if (isSOSActive) {
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, SOS già attivo")
                } else {
                    startListening()
                    //                  startSOS()
                    Log.d("RIDE MONITOR", "Accelerazione ignorata, già in ascolto vocale")
                }
            }
        }
    }

    fun vibratePhone(context: Context, durationMs: Long = 500) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    durationMs,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

}

