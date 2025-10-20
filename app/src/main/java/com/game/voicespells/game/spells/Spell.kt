package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3

abstract class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val damage: Int // For direct damage spells, can be 0 for utility spells
    abstract val cooldown: Float // in seconds
    abstract val voiceCommand: String // This should match SpellType command for consistency
    abstract val spellType: SpellType // Link to the SpellType enum

    /**
     * Executes the spell's effect.
     * @param caster The player casting the spell.
     * @param targetPosition The target position for the spell (can be a direction or point).
     *                       For spells like shields, this might not be used directly or could be caster's position.
     * @param playersInScene A list of all players, for area effects or targeting.
     */
    abstract fun execute(
        caster: Player,
        targetPosition: Vector3,
        playersInScene: List<Player> = emptyList() // Added for spells needing to know about other players
    )

    // Common logic for spells can be added here if needed
}
