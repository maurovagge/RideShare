package mau.app.rideshare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.type.LatLng

class Ride {
    @DocumentId
    val id: String? = null

    var partenza: RideStep = RideStep()
    var arrivo: RideStep = RideStep()

    var data: Timestamp = Timestamp.now()
    var posti: Int = 0
    var autista: String = ""
    var telefono: String = ""

    var stato: String = "Disponibile"


@get:Exclude
    val postiLiberi: Int
        get() = posti - viaggiatori.size
    @get:PropertyName("viaggiatori")
    @set:PropertyName("viaggiatori")
    var viaggiatori: List<Passenger> = emptyList()
    var checkin: List<String> = emptyList()
}