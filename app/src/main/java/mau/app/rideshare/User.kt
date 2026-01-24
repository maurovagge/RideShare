package mau.app.rideshare

import com.google.firebase.firestore.DocumentId

class User {
    @DocumentId
    var id: String? = null

    var Nome: String = ""

    var Telefono: String = ""

}