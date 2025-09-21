package com.game.voicespells.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.game.voicespells.domain.entities.BasicSpell
import com.game.voicespells.domain.entities.GameState
import com.game.voicespells.domain.entities.PlayerState
import com.game.voicespells.domain.entities.ProjectileState
import com.game.voicespells.domain.entities.Spell
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    private val _lastSpellCast = MutableStateFlow<Spell?>(null)
    val lastSpellCast: StateFlow<Spell?> = _lastSpellCast

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState

    private val availableSpells: Map<String, Spell> = mapOf(
        "fireball" to BasicSpell("Fireball", 10, 1f, "fireball", 30),
        "freezing" to BasicSpell("Freezing", 15, 3f, "freezing", 0),
        "lightning" to BasicSpell("Lightning", 20, 2f, "lightning", 25),
        "stone" to BasicSpell("Stone", 5, 5f, "stone", 0),
        "gust" to BasicSpell("Gust", 10, 2f, "gust", 0)
    )

    init {
        runGameLoop()
    }

    private fun runGameLoop() {
        viewModelScope.launch {
            val player = PlayerState(id = 1f, x = 0f, y = 0f)
            var angle = 0f

            while (true) {
                // Simple circular movement for the player
                angle += 0.05f
                player.x = kotlin.math.cos(angle) * 0.5f
                player.y = kotlin.math.sin(angle) * 0.5f

                val currentState = _gameState.value
                val updatedProjectiles = currentState.projectiles.map {
                    // Move projectiles (e.g., simple forward motion)
                    it.copy(x = it.x + 0.01f)
                }.filter { it.x < 1.0f } // Remove projectiles that go off-screen

                _gameState.value = currentState.copy(
                    players = listOf(player),
                    projectiles = updatedProjectiles
                )

                delay(16) // ~60 FPS
            }
        }
    }

    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            val spell = availableSpells[command]
            if (spell != null) {
                _lastSpellCast.value = spell

                // Create a projectile when a spell is cast
                val player = _gameState.value.players.firstOrNull() ?: return@launch
                val newProjectile = ProjectileState(
                    id = Random.nextFloat(),
                    x = player.x,
                    y = player.y,
                    type = 0f // 0 for fireball
                )

                _gameState.value = _gameState.value.copy(
                    projectiles = _gameState.value.projectiles + newProjectile
                )
            }
        }
    }
}
