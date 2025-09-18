package com.game.voicespells.game.entities

import com.game.voicespells.game.spells.Spell // Forward declaration, will be created next
import com.game.voicespells.utils.Vector3

/**
 * Represents a player in the game.
 * This is a placeholder and will be expanded upon.
 */
data class Player(
    var position: Vector3,
    var rotation: Float, // Assuming rotation around Y-axis for simplicity
    var hp: Int = 100,
    var mana: Int = 100,
    var selectedSpells: List<Spell> = emptyList(),
    val id: String
) {
    fun takeDamage(amount: Int) {
        hp -= amount
        if (hp < 0) hp = 0
        // Add logic for player death, etc.
    }

    fun useMana(amount: Int): Boolean {
        if (mana >= amount) {
            mana -= amount
            return true
        }
        return false
    }

    // Placeholder for castSpell - actual logic will depend on Spell implementations
    fun castSpell(spell: Spell, target: Vector3) {
        if (useMana(spell.manaCost)) {
            // spell.execute(this, target) // This will be called by the game logic/spell system
            // Actual execution might be handled by a SpellExecutionSystem
            println("Player $id casts ${spell.name} at $target")
        } else {
            println("Player $id failed to cast ${spell.name}: not enough mana")
        }
    }

    fun respawn() {
        hp = 100
        mana = 100
        // Reset position to a spawn point, etc.
        println("Player $id respawned.")
    }
}
