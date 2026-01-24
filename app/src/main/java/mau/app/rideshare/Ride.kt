package mau.app.rideshare

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

class Ride {
    @DocumentId
    val id: String? = null
    var Partenza: String = ""
    var Arrivo: String = ""
    var Data: Timestamp = Timestamp.now()
    var Posti: Int = 0
    var Autista: String = ""
    var Telefono: String = ""
@get:Exclude
    val PostiLiberi: Int
        get() = Posti - Viaggiatori.size
    @get:PropertyName("viaggiatori")
    @set:PropertyName("viaggiatori")
    var Viaggiatori: List<String> = emptyList()
}