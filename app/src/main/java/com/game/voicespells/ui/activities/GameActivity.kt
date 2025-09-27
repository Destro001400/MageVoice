package com.game.voicespells.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.game.voicespells.databinding.ActivityGameBinding
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3
import com.game.voicespells.game.spells.* // Import all basic spells
import com.game.voicespells.voice.VoiceRecognitionManager
import com.game.voicespells.utils.GameConfig
// import com.game.voicespells.network.NetworkManager // For later
// import com.game.voicespells.ui.views.GameRenderer // For later
import java.util.UUID

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityGameBinding
    private lateinit var voiceManager: VoiceRecognitionManager
    // private var gameRenderer: GameRenderer? = null // To be implemented in Phase 2
    // private var networkManager: NetworkManager? = null // To be implemented in Phase 2

    private lateinit var localPlayer: Player
    private val otherPlayers = mutableListOf<Player>() // For multiplayer later

    private val gameLoopHandler = Handler(Looper.getMainLooper())
    private var lastFrameTime: Long = 0

    // Basic Spell instances (Player's selectedSpells would ideally come from selection screen)
    private val availableSpells: Map<SpellType, Spell> = mapOf(
        SpellType.FIREBALL to Fireball(),
        SpellType.FREEZE to Freeze(),
        SpellType.LIGHTNING to Lightning(),
        SpellType.STONE to Stone(),
        SpellType.GUST to Gust()
    )

    // Touch input state for joystick and camera
    private var joystickDown = false
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var joystickCurrentX = 0f
    private var joystickCurrentY = 0f

    private var cameraTouchDown = false
    private var lastCameraTouchX = 0f
    // private var lastCameraTouchY = 0f // If vertical camera movement is also needed

    private val TAG = "GameActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Player
        // ID should be unique, perhaps from NetworkManager or a local UUID for now
        localPlayer = Player(
            position = Vector3(0f, 0f, 0f), // Starting position
            rotation = 0f,
            id = "player_${UUID.randomUUID()}",
            selectedSpells = availableSpells.values.toList() // Give all basic spells for now
        )

        // Initialize Voice Recognition
        voiceManager = VoiceRecognitionManager(this)
        setupVoiceRecognitionCallbacks()

        // Initialize UI elements and listeners
        setupUI()

        // Initialize SurfaceView for rendering (basic for now)
        binding.surfaceViewGame.holder.addCallback(this)

        // Hide system UI for fullscreen experience
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        Log.d(TAG, "GameActivity onCreate completed.")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        binding.buttonMicrophone.setOnTouchListener { _, event ->
            handleMicrophoneTouchEvent(event)
            true // Consume the event
        }

        updateHud()

        // Setup joystick placeholder touch listener
        binding.joystickLeftPlaceholder.setOnTouchListener { _, event ->
            handleJoystickTouchEvent(event, binding.joystickLeftPlaceholder)
            true
        }

        // Setup camera control placeholder touch listener
        binding.cameraControlPlaceholder.setOnTouchListener { _, event ->
            handleCameraTouchEvent(event)
            true
        }
    }

    private fun handleJoystickTouchEvent(event: MotionEvent, joystickView: View) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                joystickDown = true
                // Initial center based on where the touch started within the view, or fixed center
                joystickCenterX = joystickView.width / 2f
                joystickCenterY = joystickView.height / 2f
                joystickCurrentX = event.x
                joystickCurrentY = event.y
                 Log.d(TAG, "Joystick DOWN at $joystickCurrentX, $joystickCurrentY. Center: $joystickCenterX, $joystickCenterY")
            }
            MotionEvent.ACTION_MOVE -> {
                if (joystickDown) {
                    joystickCurrentX = event.x
                    joystickCurrentY = event.y
                    //  Log.d(TAG, "Joystick MOVE to $joystickCurrentX, $joystickCurrentY")
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                joystickDown = false
                joystickCurrentX = joystickCenterX
                joystickCurrentY = joystickCenterY
                Log.d(TAG, "Joystick UP/CANCEL")
            }
        }
    }

    private fun handleCameraTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                cameraTouchDown = true
                lastCameraTouchX = event.x
                // lastCameraTouchY = event.y
                 Log.d(TAG, "Camera touch DOWN at $lastCameraTouchX")
            }
            MotionEvent.ACTION_MOVE -> {
                if (cameraTouchDown) {
                    val deltaX = event.x - lastCameraTouchX
                    // val deltaY = event.y - lastCameraTouchY

                    // Update player rotation (camera view)
                    // Sensitivity factor might be needed
                    val rotationSensitivity = 0.2f
                    localPlayer.rotation += deltaX * rotationSensitivity
                    // Clamp rotation if necessary (e.g., localPlayer.rotation %= 360f)

                    lastCameraTouchX = event.x
                    // lastCameraTouchY = event.y
                    // Log.d(TAG, "Camera touch MOVE by $deltaX. New player rotation: ${localPlayer.rotation}")
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                cameraTouchDown = false
                Log.d(TAG, "Camera touch UP/CANCEL")
            }
        }
    }


    private fun handleMicrophoneTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d(TAG, "Mic button pressed - starting listening.")
                voiceManager.startListening()
                binding.buttonMicrophone.alpha = 0.7f // Visual feedback
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d(TAG, "Mic button released - stopping listening.")
                voiceManager.stopListening() // SpeechRecognizer might stop on its own after result/timeout
                binding.buttonMicrophone.alpha = 1.0f
            }
        }
    }

    private fun setupVoiceRecognitionCallbacks() {
        voiceManager.onSpellRecognized = { spellType ->
            Log.i(TAG, "Spell recognized: $spellType")
            if (spellType != SpellType.UNKNOWN) {
                val spellToCast = availableSpells[spellType]
                if (spellToCast != null) {
                    // Determine target position: For simplicity, cast in front of player
                    // In a real game, this would be based on camera aim or target selection
                    val castDistance = 5f // Arbitrary distance in front
                    val angleRad = Math.toRadians(localPlayer.rotation.toDouble())
                    val targetX = localPlayer.position.x + castDistance * kotlin.math.sin(angleRad).toFloat()
                    val targetZ = localPlayer.position.z + castDistance * kotlin.math.cos(angleRad).toFloat()
                    val targetPosition = Vector3(targetX, localPlayer.position.y, targetZ) // Assume Y is ground level for now

                    Log.d(TAG, "Casting ${spellToCast.name} at $targetPosition")
                    val allPlayersInScene = mutableListOf(localPlayer).apply { addAll(otherPlayers) }
                    localPlayer.castSpell(spellToCast, targetPosition, allPlayersInScene)
                    // networkManager?.sendSpellCast(spellToCast, targetPosition) // For multiplayer
                    updateHud()
                } else {
                    Toast.makeText(this, "Magia '${spellType.command}' não implementada.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Comando de voz não reconhecido.", Toast.LENGTH_SHORT).show()
            }
        }

        voiceManager.onError = { errorMsg ->
            Log.e(TAG, "Voice recognition error: $errorMsg")
            Toast.makeText(this, "Erro no reconhecimento: $errorMsg", Toast.LENGTH_LONG).show()
            binding.buttonMicrophone.alpha = 1.0f // Reset mic button visual
        }

        voiceManager.onListeningStateChanged = { isListening ->
            Log.d(TAG, "Voice listening state: $isListening")
            // Could update UI to show listening active, e.g., animate mic button
        }
    }

    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val deltaTime = if (lastFrameTime > 0) (currentTime - lastFrameTime) / 1000.0f else 0.0f
            lastFrameTime = currentTime

            updateGame(deltaTime)
            renderGame() // For now, this will just be HUD update

            gameLoopHandler.postDelayed(this, 16) // Aim for roughly 60 FPS
        }
    }

    private fun updateGame(deltaTime: Float) {
        // Player movement from joystick
        if (joystickDown) {
            val deltaX = joystickCurrentX - joystickCenterX
            val deltaY = joystickCurrentY - joystickCenterY // In screen space, Y down is positive

            // Normalize and scale joystick input
            val maxJoystickTravel = binding.joystickLeftPlaceholder.width / 3f // Max travel distance from center
            var moveMagnitude = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)

            var normalizedInputX = if (moveMagnitude > 0.01f) deltaX / moveMagnitude else 0f
            var normalizedInputZ = if (moveMagnitude > 0.01f) -deltaY / moveMagnitude else 0f // Invert Y for forward Z movement

            // Consider player's current rotation for forward/strafe movement
            // Simple forward/backward and strafe based on joystick without rotation for now:
            // To make it relative to player's view:
            val playerAngleRad = Math.toRadians(localPlayer.rotation.toDouble())
            val moveForward = normalizedInputZ * kotlin.math.cos(playerAngleRad).toFloat() - normalizedInputX * kotlin.math.sin(playerAngleRad).toFloat()
            val moveStrafe = normalizedInputZ * kotlin.math.sin(playerAngleRad).toFloat() + normalizedInputX * kotlin.math.cos(playerAngleRad).toFloat()

            // Apply movement (deltaX is strafe, deltaZ is forward/backward relative to world/camera)
            localPlayer.move(moveStrafe, moveForward, deltaTime)
        }


        localPlayer.update(deltaTime) // Handle regen, status effects expiry
        // Update other players if any
        // otherPlayers.forEach { it.update(deltaTime) }

        // Check game conditions (e.g., win/loss) - for later
        // Handle spell effects, projectiles (if any active) - for later
    }


    @SuppressLint("SetTextI18n")
    private fun updateHud() {
        binding.textViewHP.text = "HP: ${localPlayer.hp}"
        binding.textViewMana.text = "Mana: ${localPlayer.mana}"
    }

    private fun renderGame() {
        if (binding.surfaceViewGame.holder.surface.isValid) {
            // gameRenderer?.drawFrame(localPlayer, otherPlayers) // Phase 2
            updateHud() // Ensure HUD is up-to-date even if full render isn't happening
        }
    }

    // --- Game Loop Control ---
    private fun startGameLoop() {
        Log.d(TAG, "Starting game loop.")
        lastFrameTime = System.currentTimeMillis()
        gameLoopHandler.removeCallbacks(gameLoopRunnable) // Ensure no multiple loops
        gameLoopHandler.post(gameLoopRunnable)
    }

    private fun stopGameLoop() {
        Log.d(TAG, "Stopping game loop.")
        gameLoopHandler.removeCallbacks(gameLoopRunnable)
    }

    // --- Activity Lifecycle ---
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "GameActivity onResume.")
        // Re-hide system UI in case it became visible
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        if (binding.surfaceViewGame.holder.surface.isValid) {
             startGameLoop()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "GameActivity onPause.")
        stopGameLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "GameActivity onDestroy.")
        voiceManager.destroy()
        stopGameLoop() // Ensure loop is stopped
        // gameRenderer?.cleanup() // Phase 2
    }

    // --- SurfaceHolder.Callback ---
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created.")
        // gameRenderer = GameRenderer(binding.surfaceViewGame, this) // Initialize renderer - Phase 2
        startGameLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: $width x $height")
        // gameRenderer?.onSurfaceChanged(width, height) // Phase 2
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed.")
        stopGameLoop()
        // gameRenderer?.onSurfaceDestroyed() // Phase 2 - release resources
        // gameRenderer = null
    }
}
