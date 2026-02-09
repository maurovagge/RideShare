package mau.app.rideshare

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlin.getValue

class MainActivity : AppCompatActivity() {

    //declare veiw model of the application (it will be shared with all fragments)
    val viewModel: RideShareViewModel by viewModels()


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Qui gestisci cosa succede dopo che l'utente ha risposto
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Ottimo, l'utente ha accettato tutto!
        } else {
            showMandatoryPermissionsDialog()
        }
    }

    private fun showMandatoryPermissionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permessi Indispensabili")
            .setMessage("Questa applicazione è progettata per salvarti la vita. Senza accesso al GPS e alle notifiche non può funzionare. L'app verrà chiusa.")
            .setCancelable(false) // L'utente non può chiuderlo cliccando fuori
            .setPositiveButton("CHIUDI APP") { _, _ ->
                finish() // Chiude l'activity corrente
            }

    }

    private fun askAllPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val listToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (listToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(listToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        askAllPermissions()


        Log.d("SOS_DEBUG", "onCreate MainActivity")
        handleIntent(intent)


//        //Ask for required permissions
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
//                0
//            )
//        }

        //create notification channel
        NotificationUtil.initNotificationChannels(this)

        val workManager = WorkManager.getInstance(this)

        //start work manager request (check for rides every 15 minutes)
//        val periodicCheck = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
//            .build()
//        // enqueue the WorkRequest
//        workManager.enqueueUniquePeriodicWork(
//            "Memo_Ride_Share",
//            ExistingPeriodicWorkPolicy.REPLACE,
//            periodicCheck
//        )


//        val instantCheck = OneTimeWorkRequestBuilder<ReminderWorker>()
//            .build()
//        workManager.enqueue(instantCheck)


        //enable action bar arrow to navigate up
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
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

                val serviceIntent = Intent(this, RideMonitorService::class.java)
                stopService(serviceIntent)
                askForExitConfirmation()

                true
            }

            R.id.action_profile -> {
                //supportActionBar?.hide()
                val navController = findNavController(R.id.nav_host)
                navController.navigate(R.id.action_listFragment_to_profileFragment)
                true
            }


            R.id.action_SOS -> {
//                var sosId = viewModel.createSOS()
                if (viewModel.currentUser.value!!.ContattoSOS.isNotEmpty()) {
                    //activate foreground tracking service
                    val serviceIntent = Intent(this, RideMonitorService::class.java)
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
                true
            }

            R.id.action_StopSOS -> {
                val stopIntent = Intent(this, RideMonitorService::class.java).apply {
                        action = "ACTION_STOP_SOS"
                    }
                    startService(stopIntent)
                //val serviceIntent = Intent(this, SOSForegroundService::class.java)
                //stopService(serviceIntent)

                // Torna alla UI normale
                Toast.makeText(this, "Soccorso terminato", Toast.LENGTH_SHORT).show()
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

    override fun onNewIntent(intent: Intent) {
        Log.d("SOS_DEBUG", "onNewIntent MainActivity")
        super.onNewIntent(intent)
        setIntent(intent)

        handleIntent(intent);
//        val sosId = intent.getStringExtra("sosId")
//        Log.d("SOS_DEBUG", "Ricevuto nuovo sosId in singleTop: $sosId")
//
//        val uriString =  "myapp://sos_detail/$sosId"

//        val navController = findNavController(R.id.nav_host)
//        navController.handleDeepLink(intent)
    }

    private fun handleIntent(intent: Intent?) {

        if (intent == null) return
        Log.d("SOS_DEBUG", "handleIntent MainActivity")
        val intentCommand = intent.getStringExtra("COMMAND")
        if (intentCommand == null) return;
        Log.d("SOS_DEBUG", "handleIntent MainActivity: $intentCommand")
        intent.removeExtra("COMMAND")

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHostFragment.navController

        //val navController = findNavController(R.id.nav_host)
        if (intentCommand == "SOS_NOTIFICATION") {
            navController.handleDeepLink(intent)
        } else if (intentCommand == "SHOW_PROFILE") {
            navController.navigate(R.id.action_listFragment_to_profileFragment)
        }


    }

}