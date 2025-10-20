package com.game.voicespells.game.spells

import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3

class Fireball : Spell() {
    override val name: String = "Fireball"
    override val manaCost: Int = 20
    override val damage: Int = 30
    override val cooldown: Float = 2.0f // Example cooldown
    override val voiceCommand: String = "fireball"
    override val spellType: SpellType = SpellType.FIREBALL

    override fun execute(
        caster: Player,
        targetPosition: Vector3,
        playersInScene: List<Player>
    ) {
        if (caster.mana >= manaCost) {
            caster.useMana(manaCost)
            Log.d("Fireball", "${caster.id} casts Fireball towards $targetPosition!")

            // Placeholder for projectile logic:
            // 1. Create a projectile instance.
            // 2. Set its starting position (caster's position + offset).
            // 3. Set its velocity/direction towards targetPosition.
            // 4. In GameLoop/SpellSystem: update projectile position, check for collisions.
            // 5. On collision with a player (not caster), apply damage.
            // For now, we'll just log it.

            // Example: Find a target player near the targetPosition (simplified for now)
            // This is a very basic way to check; actual collision would be needed.
            playersInScene.forEach { player ->
                if (player.id != caster.id) {
                    // Simplified: check if targetPosition is roughly where another player is.
                    // A real implementation would involve projectile movement and collision detection.
                    val distance = расстояние(player.position, targetPosition) // Placeholder for distance calculation
                    if (distance < 1.0f) { // Assume 1 unit is hit radius
                        Log.d("Fireball", "Fireball hit ${player.id} for $damage damage!")
                        player.takeDamage(damage)
                    }
                }
            }

        } else {
            Log.d("Fireball", "${caster.id} tried to cast Fireball but not enough mana.")
        }
    }

    // Placeholder for a simple distance function (should be in a Vector3 or utility class)
    private fun расстояние(v1: Vector3, v2: Vector3): Float {
        val dx = v1.x - v2.x
        val dy = v1.y - v2.y
        val dz = v1.z - v2.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
