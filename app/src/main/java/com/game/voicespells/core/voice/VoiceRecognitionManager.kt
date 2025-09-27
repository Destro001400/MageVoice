package com.game.voicespells.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.game.voicespells.domain.entities.Fireball
import com.game.voicespells.domain.entities.Freezing
import com.game.voicespells.domain.entities.Gust
import com.game.voicespells.domain.entities.Lightning
import com.game.voicespells.domain.entities.Spell
import com.game.voicespells.domain.entities.Stone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages voice recognition using Android's SpeechRecognizer.
 * Based on the GDD section "SISTEMA DE RECONHECIMENTO DE VOZ".
 *
 * @param context The application context.
 */
class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    // Feedback ao Usuário (Visual): Ícone de microfone com 3 estados
    enum class RecognitionStatus { IDLE, LISTENING, PROCESSING }
    private val _status = MutableStateFlow(RecognitionStatus.IDLE)
    val status: StateFlow<RecognitionStatus> = _status

    // Now linked to the Spell data structure
    private val _recognizedSpell = MutableStateFlow<Spell?>(null)
    val recognizedSpell: StateFlow<Spell?> = _recognizedSpell

    init {
        initializeRecognizer()
    }

    /**
     * Initializes the SpeechRecognizer instance.
     */
    fun initializeRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
        } else {
            // TODO: Handle cases where recognition is not available
        }
    }

    /**
     * Starts listening for voice input.
     * The button press action should call this.
     */
    fun startListening() {
        speechRecognizer?.startListening(recognizerIntent)
        _status.value = RecognitionStatus.LISTENING
    }

    /**
     * Stops listening for voice input.
     * The button release action should call this.
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _status.value = RecognitionStatus.PROCESSING
    }

    /**
     * Cleans up the SpeechRecognizer instance.
     */
    fun destroy() {
        speechRecognizer?.destroy()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _status.value = RecognitionStatus.LISTENING
            }

            override fun onResults(results: Bundle?) {
                _status.value = RecognitionStatus.IDLE
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processCommand(matches[0])
                }
            }

            override fun onError(error: Int) {
                _status.value = RecognitionStatus.IDLE
                // TODO: Provide audio/haptic feedback for error
            }

            // Other RecognitionListener methods...
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                 _status.value = RecognitionStatus.PROCESSING
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /**
     * Processes the recognized utterance from the user.
     * @param utterance The recognized text.
     */
    private fun processCommand(utterance: String) {
        val matchedSpell = matchSpellPattern(utterance.split(" "))
        if (matchedSpell != null) {
            executeSpell(matchedSpell)
        }
    }

    /**
     * Matches the recognized words to a known spell.
     * This now returns a Spell object instead of a String.
     *
     * @param words The list of words from the utterance.
     * @return The matched Spell object or null.
     */
    private fun matchSpellPattern(words: List<String>): Spell? {
        val lowerCaseWords = words.map { it.lowercase() }
        return when {
            "fireball" in lowerCaseWords -> Fireball
            "freezing" in lowerCaseWords -> Freezing
            "lightning" in lowerCaseWords -> Lightning
            "stone" in lowerCaseWords -> Stone
            "gust" in lowerCaseWords -> Gust
            else -> null
        }
    }

    /**
     * "Executes" the spell. For now, it just updates the state.
     * This now accepts a Spell object.
     *
     * @param spell The spell to execute.
     */
    private fun executeSpell(spell: Spell) {
        _recognizedSpell.value = spell
        // TODO: Provide haptic/audio feedback for success
        // TODO: This should trigger the actual spell casting in the game logic.
    }
}
