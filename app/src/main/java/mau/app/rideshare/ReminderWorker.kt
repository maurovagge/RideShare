package mau.app.rideshare

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private var thisContext : Context = context
    companion object {
        const val CHANNEL_IDO = "ReminderWorkerChannel"
    }

    //run every 15 minutes (work manager)
    //check for incoming rides and send notification if any
    override suspend fun doWork(): Result {
        return try {
            val from =  Timestamp.now()
            val instantFrom = from.toDate().toInstant()
            val instantTo = instantFrom.plus(30, ChronoUnit.MINUTES)
            val to =  Timestamp(Date.from(instantTo))


            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                val uid = user.uid
                if (uid != null)
                {
                    val db = Firebase.firestore
                    val snapshot = db.collection("RideData")
                        .whereGreaterThanOrEqualTo("data", from)
                        .whereLessThan("data", to)
                        .whereEqualTo("autista", uid)
                        .get().await()

                    if (snapshot != null) {
                        val rides = snapshot.toObjects(Ride::class.java)
                        if (rides.isNotEmpty()) {
                            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val date = Date(rides[0].data.seconds * 1000)
                            val hhmm = sdf.format(date)
                            val msg = "Viaggio Imminente: da ${rides[0].partenza} a ${rides[0].arrivo} alle ${hhmm}"

                            Log.d("WORKER",msg)
                            sendNotification(msg)
                        }
                    }
                    Result.success()
                } else {
                    Result.success()
                }
            } else {
                return Result.success()
            }
        } catch (e: Exception) {
            Result.Failure()
        }
    }
    private fun sendNotification(message : String) {
        val notification = NotificationCompat.Builder(thisContext, CHANNEL_IDO)
            .setContentTitle("PROMEMORIA")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }
}

