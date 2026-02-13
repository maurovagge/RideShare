package mau.app.rideshare


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage



class FCMReceiver : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here
        Log.d("FCM", "From: ${remoteMessage.from}")
        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            if (remoteMessage.data.contains("sosId")) {

                val sosId = remoteMessage.data["sosId"]

                sendSOSNotification(
                    "ALLARME",
                    "Richiesta da un tuo contatto",
                    sosId)
            }
        }
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    private fun sendSOSNotification(title: String?, message: String?, sosId: String?) {
//        val intent = Intent(this, MainActivity::class.java).apply {
//            // Aggiungi questo per assicurarti che l'app si apra correttamente
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            putExtra("sosId", sosId)
//        }

        val uri = "rideshare://sos_detail/$sosId".toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {

            setClass(applicationContext, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("COMMAND", "SOS_NOTIFICATION")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )


        Log.d("FCM", "sendSOSNotification: $message")
        val notification = NotificationCompat.Builder(applicationContext, RideShareUtil.HIGH_NOTIFICATION_CHANNEL)
            .setContentTitle(title ?: "Messaggio da Firebase")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)

    }

    private fun sendNotification(title: String?, message: String?) {
        if (message == null) return

        Log.d("FCM", "sendNotification: $message")
        val notification = NotificationCompat.Builder(applicationContext, RideShareUtil.HIGH_NOTIFICATION_CHANNEL)
            .setContentTitle(title ?: "Messaggio da Firebase")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)

    }
}