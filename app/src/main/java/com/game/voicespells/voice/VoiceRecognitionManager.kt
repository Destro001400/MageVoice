package com.game.voicespells.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.game.voicespells.game.spells.SpellType
import java.util.Locale

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    var onSpellRecognized: ((SpellType) -> Unit)? = null

    private val spellCommands = mapOf(
        "fireball" to SpellType.FIREBALL,
        "freeze" to SpellType.FREEZE,
        "lightning" to SpellType.LIGHTNING,
        "stone" to SpellType.STONE,
        "gust" to SpellType.GUST
    )

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
            speechRecognizer?.setRecognitionListener(recognitionListener)
        } else {
            // Handle cases where speech recognition is not available
            // You might want to log an error or notify the user
        }
    }

    private fun setupRecognitionListener() {
        recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Handle recognition errors
                // For example, you could call onSpellRecognized with SpellType.UNKNOWN
                // or log the specific error
                onSpellRecognized?.invoke(SpellType.UNKNOWN)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0].lowercase(Locale.getDefault())
                    var spellFound = false
                    for ((command, spell) in spellCommands) {
                        if (recognizedText.contains(command)) {
                            onSpellRecognized?.invoke(spell)
                            spellFound = true
                            break
                        }
                    }
                    if (!spellFound) {
                        onSpellRecognized?.invoke(SpellType.UNKNOWN)
                    }
                } else {
                    onSpellRecognized?.invoke(SpellType.UNKNOWN)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (speechRecognizer == null) return // Speech recognizer not available

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            // EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS could be used for timeout,
            // but continuous listening is managed by starting/stopping manually.
            // For continuous mode while button is pressed, we rely on the calling UI
            // to call startListening() on ACTION_DOWN and stopListening() on ACTION_UP.
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false) // We only need final results
            // Note: True continuous recognition without stopping is complex and might require
            // restarting the listener or using other APIs if SpeechRecognizer's continuous mode is problematic.
            // For this implementation, we assume "continuous while button pressed" means the app
            // will call start/stop listening accordingly.
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: SecurityException) {
            // Handle permission issues if not already handled by requesting permissions
            onSpellRecognized?.invoke(SpellType.UNKNOWN) // Or a specific error type
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }
}
