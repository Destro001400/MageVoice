package com.game.voicespells.domain.entities

/**
 * Represents a player in the game.
 * 
 * @property hp The current health points of the player.
 * @property mana The current mana points of the player.
 * @property speed The movement speed of the player in meters per second.
 * @property isAlive Whether the player is currently alive.
 */
data class Player(
    val id: String, // Unique identifier for the player
    var hp: Int = 100,
    var mana: Int = 100,
    val speed: Float = 5.0f,
    var isAlive: Boolean = true,
    var x: Float = 0f,
    var y: Float = 0f,
    var facingAngle: Float = 0f // Angle in radians the player is facing
)
