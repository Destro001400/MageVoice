package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

/**
 * Abstract base class for all spells in the game.
 */
abstract class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val damage: Int // Base damage, could be 0 for utility spells
    abstract val cooldown: Float // Cooldown in seconds
    abstract val voiceCommand: String // The voice command to trigger this spell

    /**
     * Executes the spell's logic.
     * @param caster The player casting the spell.
     * @param target The target position or entity for the spell.
     *               Could be a specific Player or a Vector3 point in the world.
     */
    abstract fun execute(caster: Player, target: Vector3)
}
