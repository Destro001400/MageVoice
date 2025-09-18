package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Lightning : Spell() {
    override val name: String = "Lightning"
    override val manaCost: Int = 30
    override val damage: Int = 25
    override val cooldown: Float = 3.0f // Example cooldown
    override val voiceCommand: String = "lightning"

    override fun execute(caster: Player, target: Vector3) {
        // Instantaneous damage at the target point/entity.
        // In a real game, this might involve a visual effect and raycast or direct targeting.
        println("${caster.id} casts Lightning at $target, dealing $damage damage (simulated instant hit).")
        // Example: targetPlayer.takeDamage(damage) if the target is another player and hit.
    }
}
