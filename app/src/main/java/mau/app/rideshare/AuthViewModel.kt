package mau.app.rideshare

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance() // Istanza Firestore

    val isRegisterMode = MutableLiveData<Boolean>(false)

    fun toggleAuthMode() {
        isRegisterMode.value = !(isRegisterMode.value ?: false)
    }

    val isLoading = MutableLiveData<Boolean>(false)
    val authError = MutableLiveData<String?>()

    // login function
    fun login(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = { _ -> }
    ) {
        isLoading.value = true
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    val error = translateFirebaseError(task.exception)
                    onError(error)
                    authError.value = task.exception?.message
                }
            }
    }

    fun checkUsernameAvailability(
        username: String,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        val usernameRef = db.collection("Usernames").document(username)
        usernameRef.get().addOnSuccessListener { usernameDoc ->
            if (usernameDoc.exists()) {
                onError("Username già esistente - specificarne un altro")
            } else onSuccess(true)
        }.addOnFailureListener { e -> onSuccess(true) }
    }


    fun register(
        email: String,
        pass: String,
        nome: String,
        userTag: String,
        telefono: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isLoading.value = true

        // User creation on firebase auth
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userMap = hashMapOf(
                            "email" to email,
                            "nome" to nome,
                            "userTag" to userTag,
                            "telefono" to telefono,
                            "profileSaved" to true
                        )
                        val userRef = db.collection("Users").document(uid)
                        val usernameRef = db.collection("Usernames").document(userTag)

                        db.runTransaction { transaction ->
                            if (userTag.isNotEmpty()) {
                                val usernameDoc = transaction.get(usernameRef)
                                // if tag is already used
                                if (usernameDoc.exists() && usernameDoc.getString("ownerId") != uid) {
                                    throw Exception("Username già esistente - specificarne un altro")
                                }
                            }
                            // Saves username
                            if (userTag.isNotEmpty()) {
                                transaction.set(usernameRef, mapOf("ownerId" to uid))
                            }

                            transaction.set(userRef, userMap, SetOptions.merge())

                        }.addOnSuccessListener {
                            isLoading.value = false
                            onSuccess()
                        }.addOnFailureListener { e ->
                            isLoading.value = false
                            onError("Errore salvataggio dati: ${e.message}")
                        }
                    }
                } else {
                    isLoading.value = false
                    val error = translateFirebaseError(task.exception)
                    onError(error)
                    authError.value = task.exception?.message
                }
            }
    }

    private fun translateFirebaseError(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Account non trovato. Registrati per continuare."
            is FirebaseAuthInvalidCredentialsException -> "Email o password errati. Riprova."
            is FirebaseAuthUserCollisionException -> "Questa email è già registrata. Prova ad accedere."
            is FirebaseNetworkException -> "Errore di connessione. Controlla la tua rete."
            else -> "Si è verificato un errore imprevisto. Riprova più tardi."
        }
    }
}