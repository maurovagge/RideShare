package mau.app.rideshare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlin.text.get


class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Cambiato in LiveData pubblico per l'osservazione
    var currentUser = MutableLiveData<User?>()

    init {
        // Carichiamo i dati appena il ViewModel viene creato
        loadUserData()
    }

    fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        // USIAMO SEMPRE "Users" (o la collezione che hai usato nell'AuthFragment)
        db.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser.value = document.toObject(User::class.java)
                } else {
                    // Se non esiste ancora (molto raro se registrato bene), creiamo un oggetto base
                    currentUser.value = User().apply {
                        id = userId
                        email = auth.currentUser?.email ?: ""
                    }
                }
            }
            .addOnFailureListener {
                Log.e("VM_DEBUG", "Errore caricamento: ${it.message}")
            }
    }

    fun saveUserProfile(user: User, onSuccess: () -> Unit, onError: (String) -> Unit = { _ -> }) {
        val userId = auth.currentUser?.uid ?: return

        // Aggiorniamo l'ID dell'utente passato dal Fragment
        user.id = userId

        // Riferimenti corretti (Assicurati che le collezioni esistano)
        val userRef = db.collection("Users").document(userId)
        val usernameRef = db.collection("Usernames").document(user.userTag)

        db.runTransaction { transaction ->
            // Se l'utente sta salvando per la prima volta o sta cambiando tag
            if (user.userTag.isNotEmpty()) {
                val usernameDoc = transaction.get(usernameRef)
                // Se il tag è già preso da qualcun altro (ownerId diverso dal mio)
                if (usernameDoc.exists() && usernameDoc.getString("ownerId") != userId) {
                    throw Exception("Username già esistente - specificarne un altro")
                }
            }

            // Validazione Contatto SOS
            if (user.contattoSOS.isNotEmpty()) {
                val contattoSOSRef = db.collection("Usernames").document(user.contattoSOS)
                if (!transaction.get(contattoSOSRef).exists()) {
                    throw Exception("Contatto SOS non esistente")
                }
            }

            // Salvataggio Username/Tag
            if (user.userTag.isNotEmpty()) {
                transaction.set(usernameRef, mapOf("ownerId" to userId))
            }

            user.profileSaved = true
            transaction.set(userRef, user, SetOptions.merge())

        }.addOnSuccessListener {
            // IMPORTANTE: Aggiorna il LiveData con l'oggetto user che ha appena avuto successo
            currentUser.value = user
            onSuccess()
        }.addOnFailureListener { e ->
            onError(e.message ?: "Errore sconosciuto")
        }
    }
}