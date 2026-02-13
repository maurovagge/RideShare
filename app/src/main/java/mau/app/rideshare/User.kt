package mau.app.rideshare

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    var id: String = "",
    var nome: String = "",
    var userTag: String = "",
    var email: String = "",
    var telefono: String = "",
    var contattoSOS: String = "",
    var fraseSOS: String = "",
    var fraseCheckIn: String = "",
    var profileSaved: Boolean = false
)


