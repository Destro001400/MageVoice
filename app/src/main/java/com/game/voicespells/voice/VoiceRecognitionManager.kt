package com.game.voicespells.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.game.voicespells.game.spells.SpellType
import java.util.Locale

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    var onSpellRecognized: ((SpellType) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? = null // True for listening, false for not

    private val TAG = "VoiceRecognitionManager"

    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device.")
            onError?.invoke("Reconhecimento de voz não disponível.")
        } else {
            setupSpeechRecognizer()
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // EXTRA_PARTIAL_RESULTS can be useful for faster feedback but might increase processing.
            // putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1) // We only need the best match
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                onListeningStateChanged?.invoke(true)
                // Start timeout
                timeoutRunnable = Runnable {
                    Log.d(TAG, "Recognition timeout")
                    stopListening() // Stop listening if timeout is reached
                    onSpellRecognized?.invoke(SpellType.UNKNOWN) // Or a specific timeout error
                }
                handler.postDelayed(timeoutRunnable!!, 3000) // 3-second timeout
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
                // Reset timeout if speech has begun
                timeoutRunnable?.let { handler.removeCallbacks(it) }
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                onListeningStateChanged?.invoke(false)
                 // SpeechRecognizer automatically stops listening after end of speech.
                 // If continuous mode is needed while button is pressed, startListening()
                 // might need to be called again by the UI component if no result is satisfactory.
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Recognition error: $error")
                onListeningStateChanged?.invoke(false)
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                val errorMessage = getErrorText(error)
                onError?.invoke(errorMessage)
                onSpellRecognized?.invoke(SpellType.UNKNOWN) // Indicate error/unknown
            }

            override fun onResults(results: Bundle?) {
                onListeningStateChanged?.invoke(false)
                timeoutRunnable?.let { handler.removeCallbacks(it) }
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "Recognized: $recognizedText")
                    val spellType = SpellType.fromCommand(recognizedText)
                    onSpellRecognized?.invoke(spellType)
                } else {
                    Log.d(TAG, "No recognition results")
                    onSpellRecognized?.invoke(SpellType.UNKNOWN)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                 // If EXTRA_PARTIAL_RESULTS was true, handle partial results here
                 // val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                 // if (!matches.isNullOrEmpty()) {
                 //     Log.d(TAG, "Partial: ${matches[0]}")
                 // }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun startListening() {
        if (speechRecognizer == null) {
            Log.e(TAG, "Speech recognizer not initialized or not available.")
            onError?.invoke("Reconhecimento de voz não inicializado.")
            return
        }
        Log.d(TAG, "Starting listening")
        // For continuous listening while a button is pressed, the calling component
        // will call startListening on ACTION_DOWN and stopListening on ACTION_UP.
        // SpeechRecognizer itself typically stops after a single utterance or a pause.
        speechRecognizer?.startListening(recognitionIntent)
    }

    fun stopListening() {
        Log.d(TAG, "Stopping listening")
        onListeningStateChanged?.invoke(false)
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        speechRecognizer?.stopListening() // Use stopListening() for a controlled stop
                                          // Use cancel() to immediately stop and discard any pending results.
    }

    fun destroy() {
        Log.d(TAG, "Destroying speech recognizer")
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
            SpeechRecognizer.ERROR_CLIENT -> "Erro no cliente"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissões insuficientes"
            SpeechRecognizer.ERROR_NETWORK -> "Erro de rede"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout de rede"
            SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma correspondência" // This can be handled as UNKNOWN spell
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecedor ocupado"
            SpeechRecognizer.ERROR_SERVER -> "Erro no servidor"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nenhuma fala detectada (timeout)"
            else -> "Erro desconhecido no reconhecimento de voz"
        }
    }
}
