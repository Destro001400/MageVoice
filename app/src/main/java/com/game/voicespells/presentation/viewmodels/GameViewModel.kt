package com.game.voicespells.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.game.voicespells.core.voice.VoiceRecognitionManager
import com.game.voicespells.domain.entities.Player
import com.game.voicespells.domain.entities.Spell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main game screen.
 * It holds the game state and handles game logic, following the MVVM pattern.
 */
class GameViewModel : ViewModel() {

    // The local player
    private val _localPlayer = MutableStateFlow(Player(id = "local_player"))
    val localPlayer: StateFlow<Player> = _localPlayer

    // TODO: Manage a list of all players in the game
    private val _remotePlayers = MutableStateFlow<List<Player>>(emptyList())
    val remotePlayers: StateFlow<List<Player>> = _remotePlayers

    // TODO: This will be replaced by a proper game state object
    private val _lastCastedSpell = MutableStateFlow<Spell?>(null)
    val lastCastedSpell: StateFlow<Spell?> = _lastCastedSpell

    /**
     * Connects the ViewModel to the VoiceRecognitionManager.
     * The Activity should call this after initializing the voice manager.
     */
    fun observeVoiceRecognition(voiceManager: VoiceRecognitionManager) {
        viewModelScope.launch {
            voiceManager.recognizedSpell.collect { spell ->
                spell?.let { castSpell(it) }
            }
        }
    }

    /**
     * Handles the logic for casting a spell.
     * For now, it just updates the state.
     */
    private fun castSpell(spell: Spell) {
        val currentPlayer = _localPlayer.value
        if (currentPlayer.useMana(spell.manaCost)) {
            _localPlayer.value = currentPlayer.copy() // Update state
            _lastCastedSpell.value = spell

            // TODO: Implement spell effects (e.g., damage, healing, movement)
            // This is where the "Sistema de Combate" logic will be triggered.
        } else {
            // TODO: Signal that the player has not enough mana
        }
    }

    /**
     * Updates the player's position based on input.
     * This would be called by the virtual joystick.
     */
    fun movePlayer(deltaX: Float, deltaY: Float) {
        // This is a placeholder. The actual movement logic will be more complex
        // and likely handled by a physics/game engine component.
        // For now, we just log it.
        println("Moving player by ($deltaX, $deltaY)")
    }
}