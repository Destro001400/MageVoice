package com.game.voicespells.presentation.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.game.voicespells.core.voice.VoiceRecognitionManager
import com.game.voicespells.databinding.ActivityGameBinding
import com.game.voicespells.presentation.viewmodels.GameViewModel
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager

    private val recordAudioRequestCode = 101

    // JNI Functions
    private external fun initNative(surface: Surface)
    private external fun onJoystickMovedNative(x: Float, y: Float)
    private external fun cleanupNative()

    companion object {
        init {
            System.loadLibrary("magevoice")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gameSurface.holder.addCallback(this)

        setupPermissions()
        setupUI()
        observeViewModel()
    }

    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), recordAudioRequestCode)
        }
    }

    private fun setupUI() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceRecognitionManager = VoiceRecognitionManager(this)
            viewModel.observeVoiceRecognition(voiceRecognitionManager)
        }

        binding.micButton.setOnTouchListener { _, event ->
            handleMicButtonTouch(event)
            true
        }

        binding.joystickPlaceholder.setOnTouchListener { v, event ->
            handleJoystickTouch(event, v)
            true
        }
    }

    private fun handleMicButtonTouch(event: MotionEvent) {
        if (!::voiceRecognitionManager.isInitialized) return

        when (event.action) {
            MotionEvent.ACTION_DOWN -> voiceRecognitionManager.startListening()
            MotionEvent.ACTION_UP -> voiceRecognitionManager.stopListening()
        }
    }

    private fun handleJoystickTouch(event: MotionEvent, view: View) {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val centerX = view.width / 2f
                val centerY = view.height / 2f
                val inputX = event.x
                val inputY = event.y

                var deltaX = inputX - centerX
                var deltaY = inputY - centerY

                // Normalize
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                if (distance > centerX) { // Clamp to the edge of the joystick
                    deltaX = (deltaX / distance) * centerX
                    deltaY = (deltaY / distance) * centerY
                }

                // Final normalized values [-1, 1]
                val normalizedX = deltaX / centerX
                val normalizedY = deltaY / centerY

                onJoystickMovedNative(normalizedX, normalizedY)
            }
            MotionEvent.ACTION_UP -> {
                onJoystickMovedNative(0f, 0f)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.localPlayer.collect { player ->
                binding.hpText.text = "HP: ${player.hp}"
                binding.mpText.text = "MP: ${player.mana}"
            }
        }

        lifecycleScope.launch {
            viewModel.lastCastedSpell.collect { spell ->
                spell?.let {
                    println("Casted: ${it.name}")
                }
            }
        }

        if(::voiceRecognitionManager.isInitialized) {
            lifecycleScope.launch {
                voiceRecognitionManager.status.collect { status ->
                    // TODO: Update mic button visual based on status
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == recordAudioRequestCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voiceRecognitionManager = VoiceRecognitionManager(this)
            viewModel.observeVoiceRecognition(voiceRecognitionManager)
        }
    }

    override fun onDestroy() {
        cleanupNative()
        if (::voiceRecognitionManager.isInitialized) {
            voiceRecognitionManager.destroy()
        }
        super.onDestroy()
    }

    // SurfaceHolder.Callback methods
    override fun surfaceCreated(holder: SurfaceHolder) {
        initNative(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // The native code should handle this if needed
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // The native code should handle this if needed
    }
}