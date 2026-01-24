package mau.app.rideshare


import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mau.app.rideshare.R
import mau.app.rideshare.ReminderWorker.Companion.CHANNEL_ID

class FCMReceiver : FirebaseMessagingService()
{
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle FCM messages here
        Log.d("FCM","From: ${remoteMessage.from}")
        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            // Handle the data payload as needed.
        }
        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FCM", "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }
    private fun sendNotification(title: String?, message : String?) {
        if (message == null) return

        Log.d("FCM", "sendNotification: $message")
        val notification = NotificationCompat.Builder(applicationContext , CHANNEL_ID)
            .setContentTitle(title ?: "Messaggio da Firebase")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notification)

    }
}