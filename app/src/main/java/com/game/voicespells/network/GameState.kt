package com.game.voicespells.network

import com.game.voicespells.utils.Vector3
import kotlinx.serialization.Serializable

/**
 * Represents the overall state of the game, to be synchronized over the network.
 */
@Serializable
data class GameState(
    val players: Map<String, PlayerState> = emptyMap(),
    val activeSpells: List<SpellCastInfo> = emptyList(),
    val gameTime: Long = 0L
)

/**
 * Represents the state of a single player for network synchronization.
 */
@Serializable
data class PlayerState(
    val id: String,
    val position: Vector3,
    val velocity: Vector2 = Vector2(0f, 0f), // Added velocity
    val rotation: Float,
    val hp: Int,
    val mana: Int
)

/**
 * Information about a spell cast, for network synchronization.
 */
@Serializable
data class SpellCastInfo(
    val spellName: String,
    val casterId: String,
    val targetPosition: Vector3,
    val timestamp: Long = System.currentTimeMillis()
)
