package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Freeze : Spell() {
    override val name: String = "Freeze"
    override val manaCost: Int = 25
    override val damage: Int = 0 // Freeze is a utility spell, direct damage is 0
    override val cooldown: Float = 5.0f // Example cooldown
    override val voiceCommand: String = "freeze"

    // Spell-specific properties
    val areaOfEffectRadius: Float = 3.0f
    val slowFactor: Float = 0.5f // 50% slow
    val slowDuration: Float = 3.0f // 3 seconds

    override fun execute(caster: Player, target: Vector3) {
        // In a real game, this would find all entities within areaOfEffectRadius of the target point
        // and apply a slowing effect to them.
        println("${caster.id} casts Freeze at $target. Entities within ${areaOfEffectRadius}m (simulated) would be slowed by ${slowFactor*100}% for $slowDuration seconds.")
        // Example: gameSystem.applyAreaEffect(target, areaOfEffectRadius, effect = Slow(slowFactor, slowDuration))
    }
}
