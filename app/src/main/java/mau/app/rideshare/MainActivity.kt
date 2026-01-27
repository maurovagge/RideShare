package mau.app.rideshare

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import mau.app.rideshare.ReminderWorker.Companion.CHANNEL_ID
import java.util.concurrent.TimeUnit
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    //declare veiw model of the application (it will be shared with all fragments)
    val viewModel: RideShareViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val target = intent.getStringExtra("TARGET_FRAGMENT")
        if (target == "PROFILE") {
            // Ensuring the NavController is ready
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host) as NavHostFragment
            val navigator = navHostFragment.navController

            navigator.navigate(R.id.action_listFragment_to_profileFragment)
        }

        //Ask for required permissions
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }

        //create notification channel
        createNotificationChannel()

        val workManager = WorkManager.getInstance(this)

        //start work manager request (check for rides every 15 minutes)
        val periodicCheck = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
            .build()
        // enqueue the WorkRequest
        workManager.enqueueUniquePeriodicWork("Memo_Ride_Share", ExistingPeriodicWorkPolicy.REPLACE, periodicCheck)


//        val instantCheck = OneTimeWorkRequestBuilder<ReminderWorker>()
//            .build()
//        workManager.enqueue(instantCheck)


        //enable action bar arrow to navigate up
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
    }

    //create notification channel (will be used by FCM notifications and internal notifications)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for foreground service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    private var showOptionMenu = true

    fun hideOptionMenu() {
        showOptionMenu = false
        invalidateOptionsMenu()
    }

    fun showOptionMenu() {
        showOptionMenu = true
        invalidateOptionsMenu()
    }



    //inflate menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!showOptionMenu) return false // Non mostra nulla
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    //handle menu actions
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_newride -> {
               // supportActionBar?.hide()
                val navController = findNavController(R.id.nav_host)
                navController.navigate(R.id.action_listFragment_to_addRideFragment)
                true
            }

            R.id.action_exit -> {

                askForExitConfirmation()
                true
            }

            R.id.action_profile -> {
                //supportActionBar?.hide()
                val navController = findNavController(R.id.nav_host)
                navController.navigate(R.id.action_listFragment_to_profileFragment)
                true
            }


            android.R.id.home -> {
                val navController = findNavController(R.id.nav_host)
                navController.navigateUp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    fun askForExitConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("CONFERMA")
            .setMessage("Eseguire il logout?")
            .setNegativeButton("Annulla") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Si") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                finishAffinity()
            }
            .show()
    }
}