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
import com.game.voicespells.network.Action
import com.game.voicespells.network.NetworkManager
import com.game.voicespells.network.UpdatePosition
import com.game.voicespells.network.Vector2
import com.game.voicespells.presentation.viewmodels.GameViewModel
import kotlinx.coroutines.launch

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityGameBinding
    private val viewModel: GameViewModel by viewModels()
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager

    private val recordAudioRequestCode = 101
    private var localPlayerId: String? = null

    // JNI Functions
    private external fun initNative(surface: Surface)
    private external fun onJoystickMovedNative(x: Float, y: Float)
    private external fun cleanupNative()
    private external fun updatePlayerStateNative(playerId: String, x: Float, y: Float, hp: Int, mana: Int)

    companion object {
        init {
            System.loadLibrary("magevoice")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localPlayerId = intent.getStringExtra("PLAYER_ID")
        NetworkManager.init(this)

        binding.surfaceViewGame.holder.addCallback(this)

        setupPermissions()
        setupUI()
        observeViewModel()
        observeNetwork()
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

        binding.buttonMicrophone.setOnTouchListener { _, event ->
            handleMicButtonTouch(event)
            true
        }

        binding.joystickViewLeft.setOnTouchListener { v, event ->
            handleJoystickTouch(event, v)
            true
        }
    }

    fun updatePlayerOnEngine(playerId: String, x: Float, y: Float, hp: Int, mana: Int) {
        updatePlayerStateNative(playerId, x, y, hp, mana)
    }

    private fun handleMicButtonTouch(event: MotionEvent) {
        if (!::voiceRecognitionManager.isInitialized) return

        when (event.action) {
            MotionEvent.ACTION_DOWN -> voiceRecognitionManager.startListening()
            MotionEvent.ACTION_UP -> voiceRecognitionManager.stopListening()
        }
    }

    private fun handleJoystickTouch(event: MotionEvent, view: View) {
        val localId = localPlayerId ?: return

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val centerX = view.width / 2f
                val centerY = view.height / 2f
                val inputX = event.x
                val inputY = event.y

                var deltaX = inputX - centerX
                var deltaY = inputY - centerY

                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                if (distance > centerX) { // Clamp to the edge of the joystick
                    deltaX = (deltaX / distance) * centerX
                    deltaY = (deltaY / distance) * centerY
                }

                val normalizedX = deltaX / centerX
                val normalizedY = deltaY / centerY

                onJoystickMovedNative(normalizedX, normalizedY)

                val action = UpdatePosition(localId, Vector2(normalizedX, normalizedY))
                NetworkManager.sendAction(action)
            }
            MotionEvent.ACTION_UP -> {
                onJoystickMovedNative(0f, 0f)
                val action = UpdatePosition(localId, Vector2(0f, 0f))
                NetworkManager.sendAction(action)
            }
        }
    }

    private fun observeViewModel() {
        // This would be driven by the network state now
    }

    private fun observeNetwork() {
        lifecycleScope.launch {
            NetworkManager.clientGameState.collect { gameState ->
                gameState.players.forEach { (id, playerState) ->
                    updatePlayerOnEngine(
                        id,
                        playerState.position.x,
                        playerState.position.y,
                        playerState.hp,
                        playerState.mana
                    )
                }
                // You could update the ViewModel here with the local player's state
                // val localPlayerState = gameState.players[localPlayerId]
                // if (localPlayerState != null) viewModel.updateLocalPlayerState(localPlayerState)
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
        NetworkManager.disconnect()
        if (::voiceRecognitionManager.isInitialized) {
            voiceRecognitionManager.destroy()
        }
        super.onDestroy()
    }

    // SurfaceHolder.Callback methods
    override fun surfaceCreated(holder: SurfaceHolder) {
        initNative(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {}
}