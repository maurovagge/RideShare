package mau.app.rideshare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.type.LatLng

class Ride {
    @DocumentId
    val id: String? = null

    var Partenza: RideStep = RideStep()
    var Arrivo: RideStep = RideStep()

    var Steps : List<RideStep> = emptyList()
    var Data: Timestamp = Timestamp.now()
    var Posti: Int = 0
    var Autista: String = ""
    var Telefono: String = ""

    var Stato: String = "Disponibile"

    var DirectRideTime = 0
@get:Exclude
    val PostiLiberi: Int
        get() = Posti - Viaggiatori.size
    @get:PropertyName("viaggiatori")
    @set:PropertyName("viaggiatori")
    var Viaggiatori: List<String> = emptyList()
    var Checkin: List<String> = emptyList()
}