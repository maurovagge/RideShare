package mau.app.rideshare

import com.google.firebase.Timestamp

class RideStep {
    var Address: String = ""

    var AddressCoords: RideShareLocation = RideShareLocation(0.0, 0.0)

    var EstimatedTime: Timestamp? = null

    var ActualTime: Timestamp? = null
}