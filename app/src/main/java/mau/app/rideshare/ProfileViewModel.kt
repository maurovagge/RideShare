package mau.app.rideshare

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    var currentUser = MutableLiveData<User?>()

    fun saveUserProfile(user: User) {

        currentUser.value?.Nome = user.Nome
        currentUser.value?.Telefono = user.Telefono
        currentUser.value?.ContattoSOS = user.ContattoSOS
        currentUser.value?.FraseSOS = user.FraseSOS
        currentUser.value?.FraseCheckIn = user.FraseCheckIn

        val db = Firebase.firestore
        try {
            currentUser.value?.let { user ->
                db.collection("UsersTre").document(currentUser.value?.id!!).set(user)
            }

        } catch (e: Exception) {
            Log.e("RideShareViewModel", "Error saving user profile", e)
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
                        Nome = FirebaseAuth.getInstance().currentUser?.displayName ?: ""
                        Telefono = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
                    }

                    currentUser.value?.let { user ->
                        db.collection("UsersTre").document(userId).set(user)
                    }
                }
            }
    }
}