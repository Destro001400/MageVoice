package com.game.voicespells.game.spells

import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3
import kotlin.math.sqrt

class Gust : Spell() {
    override val name: String = "Gust of Wind"
    override val manaCost: Int = 10
    override val damage: Int = 0 // Gust is a utility spell for knockback
    override val cooldown: Float = 4.0f // Example cooldown
    override val voiceCommand: String = "gust"
    override val spellType: SpellType = SpellType.GUST

    private val areaOfEffectRadius: Float = 2.5f
    private val knockbackForce: Float = 5.0f // Arbitrary unit of force/distance

    override fun execute(
        caster: Player,
        targetPosition: Vector3, // Center of the gust area
        playersInScene: List<Player>
    ) {
        if (caster.mana >= manaCost) {
            caster.useMana(manaCost)
            Log.d("Gust", "${caster.id} casts Gust of Wind at $targetPosition!")

            playersInScene.forEach { player ->
                if (player.id != caster.id) { // Gust typically doesn't affect the caster
                    val distanceToTarget = calculateDistance(player.position, targetPosition)
                    if (distanceToTarget <= areaOfEffectRadius) {
                        Log.d("Gust", "Gust is affecting ${player.id}, applying knockback.")

                        // Calculate knockback direction (away from targetPosition or caster.position)
                        // For simplicity, let's say away from the center of the Gust (targetPosition)
                        val directionX = player.position.x - targetPosition.x
                        val directionY = player.position.y - targetPosition.y
                        val directionZ = player.position.z - targetPosition.z

                        // Normalize direction vector (optional, but good for consistent force)
                        val length = sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ)
                        var normalizedDx = 0f
                        var normalizedDz = 0f // Assuming knockback is primarily horizontal for simplicity in many games

                        if (length != 0f) {
                            normalizedDx = directionX / length
                            // normalizedDy = directionY / length // if vertical knockback is desired
                            normalizedDz = directionZ / length
                        } else {
                            // Player is exactly at the center, push in a default direction or caster's forward?
                            // For now, let's assume this case is rare or handled by a small minimum distance.
                        }


                        // Apply knockback (this is conceptual, Player class needs to handle movement changes)
                        // player.applyKnockback(normalizedDx * knockbackForce, normalizedDy * knockbackForce, normalizedDz * knockbackForce)
                        // For now, we'll just log the intended effect.
                        Log.d("Gust", "Knocking back ${player.id} by roughly $knockbackForce units.")
                        // In a real implementation, the player's position would be updated
                        // over a short period, or an impulse force applied if using a physics engine.
                        // player.position.x += normalizedDx * knockbackForce (Simplified, not physically accurate)
                        // player.position.z += normalizedDz * knockbackForce (Simplified)
                        player.applyKnockback(normalizedDx * knockbackForce, normalizedDz * knockbackForce)

                    }
                }
            }
        } else {
            Log.d("Gust", "${caster.id} tried to cast Gust but not enough mana.")
        }
    }

    private fun calculateDistance(v1: Vector3, v2: Vector3): Float {
        val dx = v1.x - v2.x
        val dy = v1.y - v2.y
        val dz = v1.z - v2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
