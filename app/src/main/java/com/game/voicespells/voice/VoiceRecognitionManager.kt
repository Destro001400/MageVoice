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
    private val logTag = "VoiceRecognition"
    private var speechRecognizer: SpeechRecognizer? = null
    private var listening = false
    private val handler = Handler(Looper.getMainLooper())
    private val TIMEOUT_MS = 3000L

    var onSpellRecognized: ((SpellType) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val spellCommands = mapOf(
        "fireball" to SpellType.FIREBALL,
        "freeze" to SpellType.FREEZE,
        "lightning" to SpellType.LIGHTNING,
        "stone" to SpellType.STONE,
        "gust" to SpellType.GUST
    )

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}

                    override fun onError(error: Int) {
                        val msg = "Speech error: $error"
                        Log.w(logTag, msg)
                        onError?.invoke(msg)
                        listening = false
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

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
                            if (!spellFound) onSpellRecognized?.invoke(SpellType.UNKNOWN)
                        } else {
                            onSpellRecognized?.invoke(SpellType.UNKNOWN)
                        }
                        listening = false
                    }
                })
            }
        } else {
            onError?.invoke("Speech recognition not available on this device")
        }
    }

    fun startListening() {
        if (listening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try {
            speechRecognizer?.startListening(intent)
            listening = true
            handler.postDelayed({
                if (listening) {
                    stopListening()
                    onError?.invoke("Timeout")
                }
            }, TIMEOUT_MS)
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Unknown start error")
            listening = false
        }
    }

    fun stopListening() {
        try { speechRecognizer?.stopListening() } catch (_: Exception) {}
        listening = false
        handler.removeCallbacksAndMessages(null)
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
