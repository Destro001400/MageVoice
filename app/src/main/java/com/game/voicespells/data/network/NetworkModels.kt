package com.game.voicespells.data.network

import kotlinx.serialization.Serializable

/**
 * Base class for all events sent over the network.
 */
@Serializable
sealed class GameEvent

/**
 * Represents a full snapshot of the game state.
 * Sent from the server to all clients on every tick.
 */
@Serializable
data class GameStateUpdate(
    val players: Map<String, PlayerStateDto>,
    val projectiles: List<ProjectileDto>
) : GameEvent()

/**
 * Data Transfer Object for Player state.
 */
@Serializable
data class PlayerStateDto(
    val id: String,
    val x: Float,
    val y: Float,
    val facingAngle: Float,
    val hp: Int
)

/**
 * Data Transfer Object for Projectile state.
 */
@Serializable
data class ProjectileDto(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: String
)

// --- Events sent from Client to Server ---

/**
 * Sent by a client when they move their joystick.
 */
@Serializable
data class InputState(
    val angle: Float,
    val strength: Float
) : GameEvent()

/**
 * Sent by a client when they cast a spell.
 */
@Serializable
data class SpellCastRequest(
    val spellName: String
) : GameEvent()


// --- Events managed internally by NetworkManager and observed by ViewModel ---

sealed class ConnectionEvent {
    data class ClientConnected(val clientId: String) : ConnectionEvent()
    data class ClientDisconnected(val clientId: String) : ConnectionEvent()
    data class EventReceived(val clientId: String, val event: GameEvent) : ConnectionEvent()
}
