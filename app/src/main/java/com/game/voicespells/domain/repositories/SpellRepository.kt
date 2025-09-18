package com.game.voicespells.domain.repositories

import com.game.voicespells.domain.entities.Spell

/**
 * Repository for managing spells.
 * In a real app, this might fetch spells from a local database or a remote server.
 * For the MVP, it holds a predefined list of basic spells.
 */
class SpellRepository {

    private val knownSpells = listOf(
        Spell.Basic(name = "Fireball", manaCost = 10, cooldown = 2.0f, damage = 30),
        Spell.Basic(name = "Freezing", manaCost = 15, cooldown = 3.0f, damage = 5), // Damage is low, effect is slow
        Spell.Basic(name = "Lightning", manaCost = 20, cooldown = 1.0f, damage = 25),
        Spell.Basic(name = "Stone", manaCost = 5, cooldown = 5.0f, damage = 0), // Defensive spell
        Spell.Basic(name = "Gust", manaCost = 10, cooldown = 2.5f, damage = 10) // Low damage, effect is knockback
    )

    /**
     * Finds a spell by matching its name with the given text.
     * The matching is case-insensitive.
     * 
     * @param spokenText The text spoken by the user.
     * @return The matched [Spell] or null if no match is found.
     */
    fun findSpell(spokenText: String): Spell? {
        return knownSpells.find { it.name.equals(spokenText, ignoreCase = true) }
    }

    /**
     * Gets all available spells.
     */
    fun getAllSpells(): List<Spell> = knownSpells
}
