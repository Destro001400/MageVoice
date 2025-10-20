package com.game.voicespells.game.spells

import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3
import kotlin.math.sqrt // Already imported in Fireball, but good practice to keep it per file if used.

class Freeze : Spell() {
    override val name: String = "Freeze"
    override val manaCost: Int = 25
    override val damage: Int = 0 // Freeze is a utility spell, applies slow
    override val cooldown: Float = 5.0f // Example cooldown
    override val voiceCommand: String = "freeze"
    override val spellType: SpellType = SpellType.FREEZE

    private val areaOfEffectRadius: Float = 3.0f
    private val slowPotency: Float = 0.5f // 50% slow
    private val slowDuration: Float = 3.0f // 3 seconds

    override fun execute(
        caster: Player,
        targetPosition: Vector3, // Center of the freeze area
        playersInScene: List<Player>
    ) {
        if (caster.mana >= manaCost) {
            caster.useMana(manaCost)
            Log.d("Freeze", "${caster.id} casts Freeze at $targetPosition!")

            // Affect players within the area of effect
            playersInScene.forEach { player ->
                if (player.id != caster.id) { // Usually utility spells don't affect caster unless intended
                    val distanceToTarget = calculateDistance(player.position, targetPosition)
                    if (distanceToTarget <= areaOfEffectRadius) {
                        Log.d("Freeze", "Freeze is affecting ${player.id}, applying slow.")
                        // In a real game, you would apply a "slow" status effect to the player
                        // This might involve:
                        // - player.applyStatusEffect(StatusEffect.Slow(potency = slowPotency, duration = slowDuration))
                        // - The Player class would then have logic to check active status effects
                        //   and modify its movement speed accordingly.
                        // For now, we'll just log it.
                        // You might want to pass a reference to a StatusEffectManager or similar.
                        player.applySlowEffect(slowPotency, slowDuration)
                    }
                }
            }
        } else {
            Log.d("Freeze", "${caster.id} tried to cast Freeze but not enough mana.")
        }
    }

    // Helper function for distance (can be moved to a utility class or Vector3 extension)
    private fun calculateDistance(v1: Vector3, v2: Vector3): Float {
        val dx = v1.x - v2.x
        val dy = v1.y - v2.y
        val dz = v1.z - v2.z // Assuming Z matters for AoE, if game is 3D.
                             // If it's a ground-targeted AoE in a 3D world, you might only use X and Z.
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
