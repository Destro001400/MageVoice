package com.game.voicespells.network

import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.utils.Vector3

/**
 * Represents the overall state of the game, to be synchronized over the network.
 * This is a simplified version and would likely be more complex in a full game,
 * possibly including states of all players, active spells, game time, etc.
 */
data class GameState(
    val players: Map<String, PlayerState> = emptyMap(), // Player ID to PlayerState
    val activeSpells: List<SpellCastInfo> = emptyList(),
    val gameTime: Long = 0L
    // Add other relevant game state information here
)

/**
 * Represents the state of a single player for network synchronization.
 */
data class PlayerState(
    val id: String,
    val position: Vector3,
    val rotation: Float,
    val hp: Int,
    val mana: Int
    // Potentially add velocity, current action, etc.
)

/**
 * Information about a spell cast, for network synchronization.
 */
data class SpellCastInfo(
    val spellName: String, // Or a spell ID/type
    val casterId: String,
    val targetPosition: Vector3,
    val timestamp: Long = System.currentTimeMillis()
)
