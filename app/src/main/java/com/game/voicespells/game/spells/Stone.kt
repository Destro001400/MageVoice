package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Stone : Spell() {
    override val name: String = "Stone Shield"
    override val manaCost: Int = 15
    override val damage: Int = 0 // This spell provides a shield, not direct damage
    override val cooldown: Float = 10.0f // Example cooldown
    override val voiceCommand: String = "stone"

    val shieldAmount: Int = 50
    // val shieldDuration: Float = 5.0f // Duration for the temporary shield, if applicable

    override fun execute(caster: Player, target: Vector3) {
        // This spell affects the caster, granting a temporary shield.
        // The target parameter might be unused or could specify an ally in some game designs.
        // For now, assume it always targets the caster.
        println("${caster.id} casts Stone Shield on self, gaining $shieldAmount temporary HP (simulated).")
        // In a real game: caster.applyEffect(TemporaryShield(shieldAmount, shieldDuration))
        // This might involve increasing HP temporarily or adding a separate shield HP pool.
        // For simplicity, we could directly add to HP and have a mechanism to remove it later,
        // or manage a separate shieldHP variable in the Player class.
    }
}
