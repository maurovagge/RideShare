package mau.app.rideshare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.transition.TransitionManager
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })

        supportActionBar?.hide()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            val intent = Intent(this, MainActivity::class.java)
            //intent.putExtra("USER", user)
            startActivity(intent)
            finish()
        }
        //startFirebaseSignIn()
    }

    private fun startFirebaseSignIn() {
        val welcomeSplash = findViewById<LinearLayout>(R.id.welcomSplash)
        TransitionManager.beginDelayedTransition(welcomeSplash.parent as ViewGroup)
        welcomeSplash.visibility = View.GONE

        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
        )

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            val intent = Intent(this, MainActivity::class.java)
            //intent.putExtra("USER", user)
            startActivity(intent)
        }
        else
        {

            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
            signInLauncher.launch(signInIntent)
        }
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        this.onSignInResult(res)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, MainActivity::class.java)
            val user= FirebaseAuth.getInstance().currentUser
            if(response?.isNewUser==true || user?.phoneNumber==null){
                intent.putExtra("TARGET_FRAGMENT", "PROFILE")
            }
            //intent.putExtra("USER", user)
            startActivity(intent)
            finish()
        } else {
            Log.e("FirebaseUI", "Sign in failed", response?.error)
        }
    }

}