package com.game.voicespells.presentation.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.game.voicespells.R
import com.game.voicespells.core.voice.VoiceRecognitionManager
import com.game.voicespells.presentation.viewmodels.GameViewModel
import com.google.android.games.core.GameActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GameActivity : GameActivity() {

    private val viewModel: GameViewModel by viewModels()
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager

    private val audioPermissionRequestCode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        // We call the superclass's onCreate method, and it will load the
        // native code and set up the surface for rendering.
        super.onCreate(savedInstanceState)

        voiceRecognitionManager = VoiceRecognitionManager(this)

        observeViewModel()
        requestAudioPermission()
    }

    private fun requestAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                audioPermissionRequestCode
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            launch {
                voiceRecognitionManager.voiceCommandFlow.collectLatest { command ->
                    if (command.isNotBlank()) {
                        viewModel.processVoiceCommand(command)
                    }
                }
            }

            // Collect the game state and pass it to the native renderer
            launch {
                viewModel.gameState.collectLatest { state ->
                    updatePlayers(state.playersToFloatArray())
                    updateProjectiles(state.projectilesToFloatArray())
                }
            }
        }
    }

    // JNI Methods defined in main.cpp
    private external fun updatePlayers(playersData: FloatArray)
    private external fun updateProjectiles(projectileData: FloatArray)

    override fun onDestroy() {
        voiceRecognitionManager.destroy()
        super.onDestroy()
    }

    // GameActivity provides input events through onTouchEvent, onKeyDown, etc.
    // We will use these to handle the mic button.
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // For now, let's use a simple screen tap to simulate the mic button
        if (event?.action == MotionEvent.ACTION_DOWN) {
            voiceRecognitionManager.startListening()
        } else if (event?.action == MotionEvent.ACTION_UP) {
            voiceRecognitionManager.stopListening()
        }
        return super.onTouchEvent(event)
    }

    companion object {
        // Load the native library.
        // The name "magevoice" depends on your CMakeLists.txt module name.
        init {
            System.loadLibrary("magevoice")
        }
    }
}
