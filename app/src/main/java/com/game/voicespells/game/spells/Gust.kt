package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Gust : Spell() {
    override val name: String = "Gust"
    override val manaCost: Int = 10
    override val damage: Int = 0 // Gust is primarily for knockback
    override val cooldown: Float = 4.0f // Example cooldown
    override val voiceCommand: String = "gust"

    val knockbackForce: Float = 5.0f // Arbitrary unit for force
    val areaOfEffectRadius: Float = 4.0f

    override fun execute(caster: Player, target: Vector3) {
        // Pushes enemies in an area away from the caster or a target point.
        // For simplicity, let's assume it pushes away from the caster in the direction of the target vector if it's a directional spell,
        // or pushes away from the target point if it's an AoE centered on target.
        // Let's assume it's an AoE push centered on the caster for this example.

        // The target Vector3 here could define the center of the Gust AoE.
        println("${caster.id} casts Gust centered at $target. Entities within ${areaOfEffectRadius}m (simulated) would be pushed back with force $knockbackForce.")
        // Example: gameSystem.applyAreaEffect(target, areaOfEffectRadius, effect = Knockback(caster.position, knockbackForce))
    }
}
