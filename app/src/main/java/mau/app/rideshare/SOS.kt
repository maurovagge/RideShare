package mau.app.rideshare

import androidx.navigation.ActivityNavigator
import androidx.work.WorkInfo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

class SOS {
    @DocumentId
    val id: String? = null

    var RideId: String = ""

    var SourceUser: String = ""

    var DestinationUser: String = ""

    var SOSinfo: String = ""
    var State: String = ""

    var Issued: Timestamp? = null

    var SOSLocation: RideShareLocation = RideShareLocation(0.0, 0.0)

}