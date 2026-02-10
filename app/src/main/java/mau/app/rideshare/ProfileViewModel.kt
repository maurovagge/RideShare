package mau.app.rideshare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    var currentUser = MutableLiveData<User?>()

    fun saveUserProfile(user: User, onSuccess: () -> Unit, onError: (String) -> Unit = { _ -> }) {

        currentUser.value?.Nome = user.Nome
        currentUser.value?.Telefono = user.Telefono
        currentUser.value?.ContattoSOS = user.ContattoSOS
        currentUser.value?.FraseSOS = user.FraseSOS
        currentUser.value?.FraseCheckIn = user.FraseCheckIn
        currentUser.value?.UserTag = user.UserTag





        val userRef = db.collection("UsersTre").document(currentUser.value?.id!!)
        val usernameRef = db.collection("Usernames").document(user.UserTag)
        db.runTransaction { transaction ->
            if (!user.ProfileSaved) {

                val usernameDoc = transaction.get(usernameRef)
                if (usernameDoc.exists()) {
                    throw Exception("Username già esistente - specificarne un altro" )
                }
            }
            if (!user.ContattoSOS.isEmpty()) {
                val contattoSOSRef = db.collection("Usernames").document(user.ContattoSOS)
                val contattoSOSDoc = transaction.get(contattoSOSRef)
                if (contattoSOSDoc.exists()) {
                } else {
                    throw Exception("Contatto SOS non esistente")
                }
            }
            if (!user.ProfileSaved) {
                transaction.set(usernameRef, mapOf("ownerId" to currentUser.value?.id!!))
            }
            user.ProfileSaved = true
            transaction.set(userRef, user, SetOptions.merge())

        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            if (e.message != null) {
                onError(e.message!!)
            }
        }
    }

    init {
        val db = Firebase.firestore
        getCurrentUserData()
    }

    fun getCurrentUserData() {
        val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val db = Firebase.firestore
        val documentReference =
            db.collection("UsersTre").document(userId).get().addOnCompleteListener { task ->
                val document = task.result
                if (document != null && document.exists()) {
                    currentUser.value = document.toObject(User::class.java)!!
                } else {
                    currentUser.value = User().apply {
                        id = userId
                        Email = FirebaseAuth.getInstance().currentUser?.email ?: ""

//                        Nome = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
//                        Telefono = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                    }

                    currentUser.value?.let { user ->
                        db.collection("UsersTre").document(userId).set(user)
                    }
                }
            }
    }
}