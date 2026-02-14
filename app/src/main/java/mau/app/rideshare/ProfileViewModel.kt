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

    var currentUser = MutableLiveData<User?>()

    init {
        // Loading user data on viewmodel creation
        loadUserData()
    }

    fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser.value = document.toObject(User::class.java)
                } else {
                    // if for somme reason user doesn't exist creates a basic instance
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


        user.id = userId

        val userRef = db.collection("Users").document(userId)
        val usernameRef = db.collection("Usernames").document(user.userTag)

        db.runTransaction { transaction ->
            if (user.userTag.isNotEmpty()) {
                val usernameDoc = transaction.get(usernameRef)
                // if tag is already used
                if (usernameDoc.exists() && usernameDoc.getString("ownerId") != userId) {
                    throw Exception("Username già esistente - specificarne un altro")
                }
            }

            // SOS contact validation
            if (user.contattoSOS.isNotEmpty()) {
                val contattoSOSRef = db.collection("Usernames").document(user.contattoSOS)
                if (!transaction.get(contattoSOSRef).exists()) {
                    throw Exception("Contatto SOS non esistente")
                }
            }

            // Saves username
            if (user.userTag.isNotEmpty()) {
                transaction.set(usernameRef, mapOf("ownerId" to userId))
            }

            user.profileSaved = true
            transaction.set(userRef, user, SetOptions.merge())

        }.addOnSuccessListener {
            currentUser.value = user
            onSuccess()
        }.addOnFailureListener { e ->
            onError(e.message ?: "Errore sconosciuto")
        }
    }
}