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

    private var keywordON : String = ""
    private var keywordOFF : String = ""
    var onCommandDetected: ((String) -> Unit)? = null

    fun setup(wordON : String, wordOFF : String) {
        keywordON = wordON
        keywordOFF = wordOFF
        StorageService.unpack(context, "model-it", "model",
            { m ->
                this.model = m
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


    // called when user stops speaking
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

    // called at the end of the session
    override fun onFinalResult(hypothesis: String) {
        Log.d("Vosk", "Risultato Finale: $hypothesis")

    }

    // called when user is speaking
    override fun onPartialResult(hypothesis: String) {
        // Log.d("Vosk", "Parziale: $hypothesis")
    }

    // handling errors
    override fun onError(exception: Exception) {
        Log.e("Vosk", "Errore: ${exception.message}")
    }

    // called if silence reaches time limit
    override fun onTimeout() {
        Log.d("Vosk", "Timeout raggiunto")
    }

    fun stop() {
        speechService?.stop()
        speechService?.shutdown()
    }
}