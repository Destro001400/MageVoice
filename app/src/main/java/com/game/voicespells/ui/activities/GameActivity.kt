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
import com.game.voicespells.core.network.LanMultiplayerManager
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.game.spells.Fireball
import com.game.voicespells.game.spells.Freeze
import com.game.voicespells.game.spells.Lightning
import com.game.voicespells.game.spells.Stone
import com.game.voicespells.game.spells.Gust
import com.game.voicespells.ui.views.GameRenderer
import com.game.voicespells.utils.GameConfig
import com.game.voicespells.utils.Vector3
import com.game.voicespells.voice.VoiceRecognitionManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID
import com.game.voicespells.game.mechanics.GameSingleton

class GameActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView
    private lateinit var surfaceHolder: SurfaceHolder
    private var gameThread: Thread? = null
    private var isRunning = false

    private lateinit var player: Player
    private lateinit var voiceManager: VoiceRecognitionManager
    private lateinit var gameRenderer: GameRenderer
    private lateinit var multiplayerManager: LanMultiplayerManager

    private lateinit var textViewHp: TextView
    private lateinit var textViewMana: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var bookButton: FloatingActionButton
    private lateinit var joystickArea: View
    private lateinit var voiceIndicator: View
    private lateinit var textViewVoiceStatus: TextView
    private lateinit var quickSpellsLayout: View
    private lateinit var spellCaption: TextView

    // Joystick control variables
    private var joystickCenterX = 0f
    private var joystickCenterY = 0f
    private var joystickRadius = 0f
    private var isJoystickPressed = false
    private var movementVector = Vector3(0f, 0f, 0f)

    private val allAvailableSpells: List<Spell> = listOf(
        Fireball(), Freeze(), Lightning(), Stone(), Gust()
    )
    private val activeSpellEffects = mutableListOf<Pair<Spell, Vector3>>() // For rendering spell effects

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        surfaceView = findViewById(R.id.surface_view_game)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)

        textViewHp = findViewById(R.id.text_view_hp)
        textViewMana = findViewById(R.id.text_view_mana)
    micButton = findViewById(R.id.button_mic)
    bookButton = findViewById(R.id.button_book)
    joystickArea = findViewById(R.id.view_joystick_area)
    voiceIndicator = findViewById(R.id.view_voice_indicator)
    textViewVoiceStatus = findViewById(R.id.text_view_voice_status)
    quickSpellsLayout = findViewById(R.id.layout_quick_spells)
    spellCaption = findViewById(R.id.text_view_spell_caption)
    textViewVoiceStatus.visibility = View.GONE
    spellCaption.visibility = View.GONE
    voiceIndicator.visibility = View.GONE
    setupBookButton()
    setupQuickSpells()
    private fun setupBookButton() {
        bookButton.setOnClickListener {
            Toast.makeText(this, "Abrir livro de magias (em breve)", Toast.LENGTH_SHORT).show()
            bookButton.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun setupQuickSpells() {
        val spellIds = listOf(R.id.image_spell_1, R.id.image_spell_2, R.id.image_spell_3)
        val spellTypes = listOf(
            com.game.voicespells.game.spells.SpellType.FIREBALL,
            com.game.voicespells.game.spells.SpellType.FREEZE,
            com.game.voicespells.game.spells.SpellType.LIGHTNING
        )
        spellIds.zip(spellTypes).forEach { (viewId, spellType) ->
            quickSpellsLayout.findViewById<View>(viewId).setOnClickListener {
                handleSpellRecognized(spellType)
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

        gameRenderer = GameRenderer(surfaceView)

        val startPosition = Vector3(GameConfig.MAP_SIZE / 4, 0f, GameConfig.MAP_SIZE / 4)
        player = Player(position = startPosition, rotation = 0f, id = UUID.randomUUID().toString(), selectedSpells = allAvailableSpells)

        voiceManager = VoiceRecognitionManager(this)
        voiceManager.onSpellRecognized = { spellType ->
            handleSpellRecognized(spellType)
        }

        multiplayerManager = LanMultiplayerManager(this)
        // Exemplo: criar sala automática para teste
        multiplayerManager.createRoom("Sala Local")
        multiplayerManager.joinRoom(multiplayerManager.roomManager.getCurrentRoom()?.id ?: "", player.id)
        multiplayerManager.startVoice()

        setupMicrophoneButton()
        setupJoystickArea()
        updateHud()

        // start global game world and register this player
        GameSingleton.world.players.add(player)
        GameSingleton.world.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupMicrophoneButton() {
        micButton.setOnTouchListener { _, event ->
            when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        voiceManager.startListening()
                        micButton.setImageResource(android.R.drawable.presence_audio_busy)
                        showVoiceIndicator(true)
                        playMicFeedback(true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        voiceManager.stopListening()
                        micButton.setImageResource(android.R.drawable.ic_btn_speak_now)
                        showVoiceIndicator(false)
                        playMicFeedback(false)
                        true
                    }
                    else -> false
                }
    private fun showVoiceIndicator(show: Boolean) {
        voiceIndicator.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            // Animação pulsante
            voiceIndicator.animate().scaleX(1.2f).scaleY(1.2f).setDuration(400).withEndAction {
                voiceIndicator.animate().scaleX(1f).scaleY(1f).setDuration(400).start()
            }.start()
        } else {
            voiceIndicator.animate().cancel()
            voiceIndicator.scaleX = 1f
            voiceIndicator.scaleY = 1f
        }
    }

    private fun playMicFeedback(start: Boolean) {
        // Feedback sonoro/háptico simples
        if (start) {
            micButton.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            micButton.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        } else {
            micButton.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            micButton.playSoundEffect(android.view.SoundEffectConstants.NAVIGATION_DOWN)
        }
    }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupJoystickArea() {
        joystickArea.post { // Wait for layout to be measured
            joystickCenterX = joystickArea.x + joystickArea.width / 2f
            joystickCenterY = joystickArea.y + joystickArea.height / 2f
            joystickRadius = joystickArea.width / 2f
        }

        joystickArea.setOnTouchListener { v, event ->
            val touchX = event.x
            val touchY = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    isJoystickPressed = true
                    var dx = touchX - (v.width / 2f)
                    var dy = touchY - (v.height / 2f)
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    if (distance > joystickRadius) {
                        dx = (dx / distance) * joystickRadius
                        dy = (dy / distance) * joystickRadius
                    }
                    movementVector.x = dx / joystickRadius
                    movementVector.z = dy / joystickRadius
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

    private fun updateGame() {
        if (isJoystickPressed) {
            val deltaTime = 1f / 30f
            val moveSpeed = GameConfig.PLAYER_SPEED * deltaTime
            player.position.x += movementVector.x * moveSpeed
            player.position.z += movementVector.z * moveSpeed
            player.position.x = player.position.x.coerceIn(0f, GameConfig.MAP_SIZE)
            player.position.z = player.position.z.coerceIn(0f, GameConfig.MAP_SIZE)
            if (movementVector.x != 0f || movementVector.z != 0f) {
                 player.rotation = kotlin.math.atan2(movementVector.x, movementVector.z) * (180f / kotlin.math.PI.toFloat())
            }
        }
        if (activeSpellEffects.size > 10) activeSpellEffects.removeAt(0)

        // Sincroniza estado do jogador local
        multiplayerManager.updatePlayerState(
            com.game.voicespells.core.network.GameSyncManager.PlayerState(
                id = player.id,
                x = player.position.x,
                y = player.position.y,
                z = player.position.z,
                hp = player.hp,
                mana = player.mana
            )
        )
    }

    private fun handleSpellRecognized(spellType: com.game.voicespells.game.spells.SpellType) {
        if (spellType == com.game.voicespells.game.spells.SpellType.UNKNOWN) {
            Toast.makeText(this, "Magia não reconhecida", Toast.LENGTH_SHORT).show()
            showVoiceStatus("Magia não reconhecida", false)
            showSpellCaption("Magia não reconhecida", false)
            return
        }

        val recognizedSpell = when(spellType) {
            com.game.voicespells.game.spells.SpellType.FIREBALL -> allAvailableSpells.find { it is Fireball }
            com.game.voicespells.game.spells.SpellType.FREEZE -> allAvailableSpells.find { it is Freeze }
            com.game.voicespells.game.spells.SpellType.LIGHTNING -> allAvailableSpells.find { it is Lightning }
            com.game.voicespells.game.spells.SpellType.STONE -> allAvailableSpells.find { it is Stone }
            com.game.voicespells.game.spells.SpellType.GUST -> allAvailableSpells.find { it is Gust }
            else -> null
        }

        if (recognizedSpell != null && player.mana >= recognizedSpell.manaCost) {
            Toast.makeText(this, "Magia: ${recognizedSpell.name}", Toast.LENGTH_SHORT).show()
            val lookDirX = kotlin.math.sin(player.rotation * (kotlin.math.PI / 180f)).toFloat()
            val lookDirZ = kotlin.math.cos(player.rotation * (kotlin.math.PI / 180f)).toFloat()
            val targetDistance = 5f
            val targetPosition = Vector3(
                player.position.x + lookDirX * targetDistance,
                player.position.y,
                player.position.z + lookDirZ * targetDistance
            )

            player.castSpell(recognizedSpell, targetPosition)
            activeSpellEffects.add(recognizedSpell to targetPosition)

            multiplayerManager.addSpellEvent(
                com.game.voicespells.core.network.GameSyncManager.SpellEvent(
                    casterId = player.id,
                    spellName = recognizedSpell.name,
                    targetX = targetPosition.x,
                    targetY = targetPosition.y,
                    targetZ = targetPosition.z
                )
            )
            updateHud()
            showVoiceStatus("Magia lançada: ${recognizedSpell.name}", true)
            showSpellCaption("Magia lançada: ${recognizedSpell.name}", true)
        } else {
            Toast.makeText(this, "Mana insuficiente para ${recognizedSpell.name}", Toast.LENGTH_SHORT).show()
            showVoiceStatus("Mana insuficiente para ${recognizedSpell.name}", false)
            showSpellCaption("Mana insuficiente para ${recognizedSpell.name}", false)
        }
    private fun showSpellCaption(msg: String, success: Boolean) {
        spellCaption.text = msg
        spellCaption.visibility = View.VISIBLE
        spellCaption.setBackgroundColor(if (success) 0x8000FF00.toInt() else 0x80FF0000.toInt())
        spellCaption.animate().alpha(1f).setDuration(200).withEndAction {
            spellCaption.animate().alpha(0f).setDuration(1200).withEndAction {
                spellCaption.visibility = View.GONE
            }.start()
        }.start()
        if (success) {
            spellCaption.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            spellCaption.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        } else {
            spellCaption.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
            spellCaption.playSoundEffect(android.view.SoundEffectConstants.NAVIGATION_DOWN)
        }
    }
    private fun showVoiceStatus(msg: String, success: Boolean) {
        textViewVoiceStatus.text = msg
        textViewVoiceStatus.visibility = View.VISIBLE
        textViewVoiceStatus.setBackgroundColor(if (success) 0x8000FF00.toInt() else 0x80FF0000.toInt())
        textViewVoiceStatus.animate().alpha(1f).setDuration(200).withEndAction {
            textViewVoiceStatus.animate().alpha(0f).setDuration(1200).withEndAction {
                textViewVoiceStatus.visibility = View.GONE
            }.start()
        }.start()
        if (success) {
            textViewVoiceStatus.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            textViewVoiceStatus.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        } else {
            textViewVoiceStatus.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
            textViewVoiceStatus.playSoundEffect(android.view.SoundEffectConstants.NAVIGATION_DOWN)
        }
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
            val sleepTime = (1000L / 30) - timeMillis

            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime) } catch (e: InterruptedException) { isRunning = false }
            }
        }
    }

    private fun drawGame() {
        if (surfaceHolder.surface.isValid) {
            val canvas = surfaceHolder.lockCanvas()
            if (canvas != null) {
                try {
                    // Renderiza todos os jogadores sincronizados
                    val allStates = multiplayerManager.getPlayerStates()
                    allStates.forEach { state =>
                        // Renderiza cada jogador
                        val p = Player(
                            position = Vector3(state.x, state.y, state.z),
                            rotation = 0f,
                            id = state.id,
                            selectedSpells = allAvailableSpells
                        )
                        gameRenderer.draw(canvas, p, activeSpellEffects)
                    }
                    // Renderiza magias sincronizadas
                    val allSpells = multiplayerManager.getSpellEvents()
                    allSpells.forEach { event =>
                        val spell = allAvailableSpells.find { it.name == event.spellName }
                        if (spell != null) {
                            val pos = Vector3(event.targetX, event.targetY, event.targetZ)
                            gameRenderer.draw(canvas, spell, pos)
                        }
                    }
                    val projs = GameSingleton.world.projectilePool.activeProjectiles()
                    gameRenderer.drawProjectiles(canvas, projs)
                } finally { surfaceHolder.unlockCanvasAndPost(canvas) }
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (gameThread?.isAlive != true) {
            isRunning = true
            gameThread = Thread(this::gameLoop)
            gameThread?.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        joystickArea.post {
            joystickCenterX = joystickArea.x + joystickArea.width / 2f
            joystickCenterY = joystickArea.y + joystickArea.height / 2f
            joystickRadius = joystickArea.width / 2f
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        isRunning = false
        while (retry) {
            try { gameThread?.join(); retry = false } catch (e: InterruptedException) { }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isRunning && surfaceHolder.surface.isValid) surfaceCreated(surfaceHolder)
    }

    override fun onPause() {
        super.onPause()
        isRunning = false
        try { gameThread?.join(500) } catch (e: InterruptedException) { e.printStackTrace() }
        GameSingleton.world.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
        isRunning = false
        gameThread?.interrupt()
        multiplayerManager.stopVoice()
        GameSingleton.world.stop()
    }
}
