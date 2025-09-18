package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Fireball : Spell() {
    override val name: String = "Fireball"
    override val manaCost: Int = 20
    override val damage: Int = 30
    override val cooldown: Float = 2.0f // Example cooldown
    override val voiceCommand: String = "fireball"

    override fun execute(caster: Player, target: Vector3) {
        // In a real game, this would create a projectile instance,
        // and a game system would handle its movement and collision.
        // For now, we'll just print a message.
        println("${caster.id} casts Fireball towards $target dealing $damage damage (simulated).")
        // Example: targetPlayer.takeDamage(damage) if the target is another player and hit.
    }
}
