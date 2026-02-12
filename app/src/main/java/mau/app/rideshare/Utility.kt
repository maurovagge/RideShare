package mau.app.rideshare
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.google.android.libraries.places.api.model.Place

object NotificationUtil {
    const val LOW_NOTIFICATION_CHANNEL = "low_notification_channel"
    const val HIGH_NOTIFICATION_CHANNEL = "high_notification_channel"

    fun initNotificationChannels(context: Context) {

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Channel for tracking data on SOS
        val sosChannel = NotificationChannel(
            LOW_NOTIFICATION_CHANNEL,
            "Low Prio Notifications",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Low Prio Notifications"
            setShowBadge(false)
        }

        // Channel for push notification (received from FCM)
        val alertChannel = NotificationChannel(
            HIGH_NOTIFICATION_CHANNEL,
            "High Prio Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High Prio Notifications"
            enableVibration(true)
        }
        manager.createNotificationChannels(listOf(sosChannel, alertChannel))
    }


    fun getPlaceName(place: Place) : String
    {
        var fullName = ""
        if (place.addressComponents != null) {
            val components = place.addressComponents?.asList()


            var via = ""
            var civico = ""
            var comune = ""

            for (component in components!!) {
                val types = component.types

                when {
                    // Via/Strada
                    types.contains("route") -> via = component.name

                    // Numero Civico
                    types.contains("street_number") -> civico = component.name

                    // Comune (Locality)
                    types.contains("locality") -> comune = component.name
                }
            }
            if (comune.isNotEmpty())
            {
                fullName += comune
                if (via.isNotEmpty())
                {
                    fullName += ", $via"
                }
                if (civico.isNotEmpty())
                {
                    fullName += " $civico"
                }
            }
            else
            {
                fullName = place.displayName
            }
            return fullName
        }
        return fullName
    }
}