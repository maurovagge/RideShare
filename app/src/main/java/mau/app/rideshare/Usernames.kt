package mau.app.rideshare

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName

class Usernames {
    private val db = FirebaseFirestore.getInstance()

    fun searchUsernames(query: String, callback: (List<String>) -> Unit) {
        if (query.isBlank()) {
            callback(emptyList())
            return
        }

        db.collection("Usernames")
            .orderBy(FieldPath.documentId())
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(4)
            .get()
            .addOnSuccessListener { snapshots ->
                val list = snapshots.documents.map { it.id }
                callback(list)
            }
    }
}


