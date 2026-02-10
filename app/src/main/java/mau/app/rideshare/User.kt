package mau.app.rideshare

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

class User {
    @DocumentId
    var id: String? = null

    var Email: String = ""

    var Nome: String = ""

    var Telefono: String = ""

    var ContattoSOS : String = ""

    var FraseSOS : String = ""

    var FraseCheckIn : String = ""

    var UserTag : String = ""

    var ProfileSaved : Boolean = false
}


