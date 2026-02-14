import android.R
import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService


class VoiceCommandManager(private val context: Context) : RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    //private val settings = SettingsManager(context)

    private var keywordON : String = ""
    private var keywordOFF : String = ""
    var onCommandDetected: ((String) -> Unit)? = null

    fun setup(wordON : String, wordOFF : String) {
        keywordON = wordON
        keywordOFF = wordOFF
        StorageService.unpack(context, "model-it", "model",
            { m ->
                this.model = m
                //startWithKeyword(keyword) // Parte in automatico con la parola salvata
            },
            { e -> Log.e("Vosk", "Errore: ${e.message}") }
        )
    }

    fun start(wordON : String, wordOFF : String) {
        keywordON = wordON
        keywordOFF = wordOFF

        if ((keywordON.isEmpty()) && (keywordOFF.isEmpty()))
            return

        val keywordsJson = """["$keywordON", "$keywordOFF", "[unk]"]"""
        model?.let {
            val rec = Recognizer(it, 16000.0f, keywordsJson)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
            Log.d("Vosk", "In ascolto per: $keywordON, $keywordOFF")
        }
    }


    // 1. Chiamato quando l'utente smette di parlare (risultato finale)
    override fun onResult(hypothesis: String) {
        Log.d("Vosk", "Risultato: $hypothesis")
        //val keyword = settings.getCommand()
        if (hypothesis.contains(keywordON)) {
            onCommandDetected?.invoke(keywordON)

        }
        else if (hypothesis.contains(keywordOFF)) {
            onCommandDetected?.invoke(keywordOFF)
        }
    }

    // 2. Chiamato alla fine della sessione (spesso identico a onResult)
    override fun onFinalResult(hypothesis: String) {
        Log.d("Vosk", "Risultato Finale: $hypothesis")

    }

    // 3. Chiamato mentre l'utente sta parlando (anteprima)
    override fun onPartialResult(hypothesis: String) {
        // Log.d("Vosk", "Parziale: $hypothesis")
    }

    // 4. Gestione degli errori (fondamentale per il debug)
    override fun onError(exception: Exception) {
        Log.e("Vosk", "Errore: ${exception.message}")
    }

    // 5. Chiamato se viene raggiunto un limite di tempo senza parlato
    override fun onTimeout() {
        Log.d("Vosk", "Timeout raggiunto")
    }

    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
    }
}

//class SettingsManager(context: Context) {
//    private val prefs = context.getSharedPreferences("voice_prefs", Context.MODE_PRIVATE)
//
//    fun saveCommand(keyword: String) {
//        prefs.edit().putString("custom_keyword", keyword.lowercase().trim()).apply()
//    }
//
//    fun getCommand(): String {
//        // "luce" è il comando di default se non c'è nulla di salvato
//        return prefs.getString("custom_keyword", "luce") ?: "luce"
//    }
//}