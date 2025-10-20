package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3 // CORRECTED IMPORT

abstract class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val damage: Int
    abstract val cooldown: Float
    abstract val voiceCommand: String
    abstract val spellType: SpellType

    /**
     * Executes the spell's effect.
     * @param caster The player casting the spell.
     * @param targetPosition The target position for the spell (can be a direction or point).
     */
    abstract fun execute(
        caster: Player,
        targetPosition: Vector3,
        playersInScene: List<Player> = emptyList()
    )
}