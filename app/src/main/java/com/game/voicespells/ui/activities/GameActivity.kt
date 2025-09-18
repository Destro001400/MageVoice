package com.game.voicespells.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.game.voicespells.R
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.spells.SpellType
import com.game.voicespells.utils.GameConfig
import com.game.voicespells.utils.Vector3
import com.game.voicespells.voice.VoiceRecognitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var gameThread: Thread? = null
    private var isRunning = false

    private lateinit var player: Player
    private lateinit var voiceManager: VoiceRecognitionManager

    private lateinit var textViewHp: TextView
    private lateinit var textViewMana: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var joystickArea: View

    // Joystick control variables
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var joystickRadius = 0f
    private var isJoystickPressed = false
    private var movementVector = Vector3(0f, 0f, 0f) // X for horizontal, Y for vertical/depth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        surfaceView = findViewById(R.id.surface_view_game)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        textViewHp = findViewById(R.id.text_view_hp)
        textViewMana = findViewById(R.id.text_view_mana)
        micButton = findViewById(R.id.button_mic)
        joystickArea = findViewById(R.id.view_joystick_area)

        // Initialize Player
        val startPosition = Vector3(GameConfig.MAP_SIZE / 2, 0f, GameConfig.MAP_SIZE / 2)
        player = Player(position = startPosition, rotation = 0f, id = UUID.randomUUID().toString())

        // Initialize Voice Recognition
        voiceManager = VoiceRecognitionManager(this)
        voiceManager.onSpellRecognized = {
            handleSpellRecognized(it)
        }

        setupMicrophoneButton()
        setupJoystickArea()
        updateHud()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicrophoneButton() {
        micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    voiceManager.startListening()
                    micButton.setImageResource(android.R.drawable.presence_audio_busy) // Indicate listening
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    voiceManager.stopListening()
                    micButton.setImageResource(android.R.drawable.ic_btn_speak_now) // Reset icon
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupJoystickArea() {
        joystickArea.post { // Wait for layout to be measured
            val location = IntArray(2)
            joystickArea.getLocationOnScreen(location)
            joystickCenterX = location[0] + joystickArea.width / 2f
            joystickCenterY = location[1] + joystickArea.height / 2f
            joystickRadius = joystickArea.width / 2f // Assuming a circular joystick area
        }

        // Use the parent view for touch events to cover joystick and camera areas if needed
        // For simplicity, direct touch on joystickArea
        joystickArea.setOnTouchListener { _, event ->
            val touchX = event.rawX
            val touchY = event.rawY

            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    isJoystickPressed = true
                    var dx = touchX - joystickCenterX
                    var dy = touchY - joystickCenterY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (distance > joystickRadius) {
                        dx = (dx / distance) * joystickRadius
                        dy = (dy / distance) * joystickRadius
                    }
                    // Normalize and scale to movement speed. Assuming dx for X, dy for Z (depth)
                    movementVector.x = dx / joystickRadius
                    movementVector.z = dy / joystickRadius // In typical 3D, Y is up, Z is depth
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isJoystickPressed = false
                    movementVector.x = 0f
                    movementVector.z = 0f
                    true
                }
                else -> false
            }
        }
    }


    private fun handleSpellRecognized(spellType: SpellType) {
        if (spellType != SpellType.UNKNOWN) {
            Toast.makeText(this, "Spell recognized: ${spellType.name}", Toast.LENGTH_SHORT).show()
            // Find the spell object from player's selectedSpells or a global list
            // For now, let's assume we have a way to get a Spell object by type
            // val spellToCast = getSpellByType(spellType)
            // if (spellToCast != null && player.mana >= spellToCast.manaCost) {
            //     player.castSpell(spellToCast, /* target position */ Vector3(player.position.x + 5f, 0f, player.position.z))
            //     networkManager.sendSpellCast(spellToCast, /* target position */)
            //     updateHud()
            // } else if (spellToCast != null) {
            //     Toast.makeText(this, "Not enough mana for ${spellToCast.name}", Toast.LENGTH_SHORT).show()
            // }
        } else {
            Toast.makeText(this, "Spell not recognized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateHud() {
        textViewHp.text = "HP: ${player.hp}"
        textViewMana.text = "Mana: ${player.mana}"
    }

    private fun gameLoop() {
        while (isRunning) {
            val startTime = System.currentTimeMillis()

            updateGame()
            drawGame()

            val timeMillis = System.currentTimeMillis() - startTime
            val sleepTime = (1000L / 60) - timeMillis // Target 60 FPS

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime)
                } catch (e: InterruptedException) {
                    // Thread interrupted, handle if necessary
                }
            }
        }
    }

    private fun updateGame() {
        // Update player position based on joystick input
        if (isJoystickPressed) {
            val delta = GameConfig.PLAYER_SPEED * (1f/60f) // Speed per frame
            player.position.x += movementVector.x * delta
            player.position.z += movementVector.z * delta // Moving in XZ plane

            // Clamp player position to map boundaries (simplified)
            player.position.x = player.position.x.coerceIn(0f, GameConfig.MAP_SIZE)
            player.position.z = player.position.z.coerceIn(0f, GameConfig.MAP_SIZE)

            // Update rotation based on movement direction (optional, simplified)
            if (movementVector.x != 0f || movementVector.z != 0f) {
                player.rotation = kotlin.math.atan2(movementVector.x, movementVector.z) * (180f / kotlin.math.PI.toFloat())
            }
        }
        // Other game logic (mana/hp regen, cooldowns, etc.) would go here
        // For now, just update HUD if values change
        // updateHud() // Called directly after player stats change or periodically
    }

    private fun drawGame() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                // For now, just clear the canvas or draw very basic info
                // In a real game, GameRenderer would draw here
                canvas.drawColor(android.graphics.Color.DKGRAY) // Placeholder background
                
                // Example: Draw player as a circle
                val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLUE }
                // Map game coordinates to screen coordinates (very simplified)
                val screenX = player.position.x * (canvas.width / GameConfig.MAP_SIZE)
                val screenY = player.position.z * (canvas.height / GameConfig.MAP_SIZE) // Using Z for Y on 2D screen
                canvas.drawCircle(screenX, screenY, 20f, paint)

                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this::gameLoop)
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // Handle surface changes if necessary
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRunning && gameThread?.isAlive == false) {
            gameThread = Thread(this::gameLoop)
            gameThread?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        isRunning = false // Stop game loop when activity is paused
        try {
            gameThread?.join(500) // Wait for thread to finish, with timeout
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
        isRunning = false // Ensure thread stops
        gameThread?.interrupt() // Force interrupt if join fails
    }
}
