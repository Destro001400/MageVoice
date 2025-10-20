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
import com.game.voicespells.utils.Vector3
import com.game.voicespells.game.spells.*
import com.game.voicespells.ui.views.JoystickView
import com.game.voicespells.ui.views.GameRenderer // Import GameRenderer
import com.game.voicespells.voice.VoiceRecognitionManager
import com.game.voicespells.utils.GameConfig
// import com.game.voicespells.network.NetworkManager // For later
import java.util.UUID

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityGameBinding
    private lateinit var voiceManager: VoiceRecognitionManager
    private var gameRenderer: GameRenderer? = null // Descomentado
    // private var networkManager: NetworkManager? = null

    private lateinit var localPlayer: Player
    private val otherPlayers = mutableListOf<Player>()

    private val gameLoopHandler = Handler(Looper.getMainLooper())
    private var lastFrameTime: Long = 0

    private val availableSpells: Map<SpellType, Spell> = mapOf(
        SpellType.FIREBALL to Fireball(),
        SpellType.FREEZE to Freeze(),
        SpellType.LIGHTNING to Lightning(),
        SpellType.STONE to Stone(),
        SpellType.GUST to Gust()
    )

    private var joystickInputNormalizedX: Float = 0f
    private var joystickInputNormalizedY: Float = 0f

    private var cameraTouchDown = false
    private var lastCameraTouchX = 0f

    private val TAG = "GameActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        localPlayer = Player(
            position = Vector3(0f, 0f, 0f),
            rotation = 0f,
            id = "player_${UUID.randomUUID()}",
            selectedSpells = availableSpells.values.toList()
        )

        voiceManager = VoiceRecognitionManager(this)
        setupVoiceRecognitionCallbacks()
        setupUI() // updateHud() será chamado pelo renderer agora
        binding.surfaceViewGame.holder.addCallback(this)

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
            true
        }

        // updateHud() // Removido - GameRenderer vai cuidar disso

        binding.joystickViewLeft.setJoystickListener(object : JoystickView.JoystickListener {
            override fun onJoystickMoved(xPercent: Float, yPercent: Float, angle: Double, strength: Float) {
                joystickInputNormalizedX = xPercent
                joystickInputNormalizedY = yPercent
            }

            override fun onJoystickReleased() {
                joystickInputNormalizedX = 0f
                joystickInputNormalizedY = 0f
                Log.d(TAG, "Joystick Released")
            }
        })

        binding.cameraControlPlaceholder.setOnTouchListener { _, event ->
            handleCameraTouchEvent(event)
            true
        }
    }

    private fun handleCameraTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                cameraTouchDown = true
                lastCameraTouchX = event.x
            }
            MotionEvent.ACTION_MOVE -> {
                if (cameraTouchDown) {
                    val deltaX = event.x - lastCameraTouchX
                    val rotationSensitivity = 0.2f
                    localPlayer.rotation += deltaX * rotationSensitivity
                    localPlayer.rotation %= 360f
                    if (localPlayer.rotation < 0) localPlayer.rotation += 360f
                    lastCameraTouchX = event.x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                cameraTouchDown = false
            }
        }
    }

    private fun handleMicrophoneTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                voiceManager.startListening()
                binding.buttonMicrophone.alpha = 0.7f
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                voiceManager.stopListening()
                binding.buttonMicrophone.alpha = 1.0f
            }
        }
    }

    private fun setupVoiceRecognitionCallbacks() {
        voiceManager.onSpellRecognized = { spellType ->
            if (spellType != SpellType.UNKNOWN) {
                val spellToCast = availableSpells[spellType]
                if (spellToCast != null) {
                    val castDistance = 5f
                    val angleRad = Math.toRadians(localPlayer.rotation.toDouble())
                    val targetX = localPlayer.position.x + castDistance * kotlin.math.sin(angleRad).toFloat()
                    val targetZ = localPlayer.position.z + castDistance * kotlin.math.cos(angleRad).toFloat()
                    val targetPosition = Vector3(targetX, localPlayer.position.y, targetZ)
                    val allPlayersInScene = mutableListOf(localPlayer).apply { addAll(otherPlayers) }
                    localPlayer.castSpell(spellToCast, targetPosition, allPlayersInScene)
                    // updateHud() // Removido - GameRenderer vai cuidar disso
                } else {
                    Toast.makeText(this, "Magia '${spellType.command}' não implementada.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Comando de voz não reconhecido.", Toast.LENGTH_SHORT).show()
            }
        }
        voiceManager.onError = { errorMsg ->
            Toast.makeText(this, "Erro no reconhecimento: $errorMsg", Toast.LENGTH_LONG).show()
            binding.buttonMicrophone.alpha = 1.0f
        }
        // ... (onListeningStateChanged unchanged)
    }

    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val deltaTime = if (lastFrameTime > 0) (currentTime - lastFrameTime) / 1000.0f else 0.0f
            lastFrameTime = currentTime

            updateGame(deltaTime)
            renderGame()

            gameLoopHandler.postDelayed(this, 16)
        }
    }

    private fun updateGame(deltaTime: Float) {
        if (joystickInputNormalizedX != 0f || joystickInputNormalizedY != 0f) {
            val inputForward = -joystickInputNormalizedY
            val inputStrafe = joystickInputNormalizedX
            val playerAngleRad = Math.toRadians(localPlayer.rotation.toDouble())
            val moveForwardComponentZ = inputForward * kotlin.math.cos(playerAngleRad).toFloat()
            val moveForwardComponentX = inputForward * kotlin.math.sin(playerAngleRad).toFloat()
            val moveStrafeComponentZ = -inputStrafe * kotlin.math.sin(playerAngleRad).toFloat()
            val moveStrafeComponentX = inputStrafe * kotlin.math.cos(playerAngleRad).toFloat()
            val worldDeltaX = moveForwardComponentX + moveStrafeComponentX
            val worldDeltaZ = moveForwardComponentZ + moveStrafeComponentZ
            localPlayer.move(worldDeltaX, worldDeltaZ, deltaTime)
        }
        localPlayer.update(deltaTime)
        // updateHud() // Removido - GameRenderer vai cuidar disso
    }

    // Removido - GameRenderer vai cuidar disso, ou os TextViews serão removidos do layout XML
    // @SuppressLint("SetTextI18n")
    // private fun updateHud() {
    //    binding.textViewHP.text = "HP: ${localPlayer.hp}"
    //    binding.textViewMana.text = "Mana: ${localPlayer.mana}"
    // }

    private fun renderGame() {
        // A lógica de updateHud() foi movida para dentro do GameRenderer.drawFrame()
        // ou os TextViews serão removidos se o GameRenderer desenhar o HUD diretamente.
        // Por agora, GameRenderer.drawFrame() já inclui drawHUD().
        gameRenderer?.drawFrame(localPlayer, otherPlayers)
    }

    private fun startGameLoop() {
        Log.d(TAG, "Starting game loop.")
        lastFrameTime = System.currentTimeMillis()
        gameLoopHandler.removeCallbacks(gameLoopRunnable)
        gameLoopHandler.post(gameLoopRunnable)
    }

    private fun stopGameLoop() {
        Log.d(TAG, "Stopping game loop.")
        gameLoopHandler.removeCallbacks(gameLoopRunnable)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "GameActivity onResume.")
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
        stopGameLoop()
        gameRenderer?.cleanup() // Adicionado
    }

    // --- SurfaceHolder.Callback ---
    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "Surface created.")
        if (gameRenderer == null) {
            gameRenderer = GameRenderer(holder, this) // Inicializa o renderer
        }
        startGameLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: $width x $height")
        gameRenderer?.onSurfaceChanged(width, height) // Informa o renderer sobre a mudança de tamanho
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "Surface destroyed.")
        stopGameLoop()
        // gameRenderer?.cleanup() // Pode ser chamado aqui ou em onDestroy
        gameRenderer = null // Libera a referência
    }
}
