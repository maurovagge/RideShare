package mau.app.rideshare

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()


    val isRegisterMode = MutableLiveData<Boolean>(false)

    fun toggleAuthMode() {
        isRegisterMode.value = !(isRegisterMode.value ?: false)
    }

    // Stato della UI (es. mostrare una barra di caricamento)
    val isLoading = MutableLiveData<Boolean>(false)
    val authError = MutableLiveData<String?>()

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit = { _ -> }) {
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

    private fun translateFirebaseError(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "Account non trovato. Registrati per continuare."
            is FirebaseAuthInvalidCredentialsException -> "Email o password errati. Riprova."
            is FirebaseAuthUserCollisionException -> "Questa email è già registrata. Prova ad accedere."
            is FirebaseNetworkException -> "Errore di connessione. Controlla la tua rete."
            else -> "Si è verificato un errore imprevisto. Riprova più tardi."
        }
    }


    fun register(email: String, pass: String, onSuccess: () -> Unit) {
        isLoading.value = true
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    authError.value = task.exception?.message
                }
            }
    }

}