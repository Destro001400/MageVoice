package com.game.voicespells.domain.entities

/**
 * Represents a projectile in the game world, like a fireball.
 *
 * @property id A unique identifier for the projectile.
 * @property x The projectile's position on the X-axis.
 * @property y The projectile's position on the Y-axis.
 * @property angle The angle in radians at which the projectile is traveling.
 * @property speed The speed of the projectile.
 * @property type The type of spell that created this projectile (e.g., "Fireball").
 */
data class Projectile(
    val id: Long,
    val x: Float,
    val y: Float,
    val angle: Float,
    val speed: Float = 20f, // Projectiles are faster than players
    val type: String
)
