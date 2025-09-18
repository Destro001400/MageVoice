package com.game.voicespells.domain.entities

/**
 * Represents a spell that can be cast by a player.
 * This is a sealed class, allowing for a restricted set of spell types,
 * as defined in the Game Design Document.
 */
sealed class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val cooldown: Float

    /**
     * Represents a basic, single-word spell.
     */
    data class Basic(
        override val name: String,
        override val manaCost: Int,
        override val cooldown: Float,
        val damage: Int
    ) : Spell()

    /**
     * Represents an advanced spell.
     */
    data class Advanced(
        override val name: String,
        override val manaCost: Int,
        override val cooldown: Float
        // TODO: Add specific properties for advanced spells
    ) : Spell()

    /**
     * Represents a special spell.
     */
    data class Special(
        override val name: String,
        override val manaCost: Int,
        override val cooldown: Float
        // TODO: Add specific properties for special spells
    ) : Spell()

    /**
     * Represents a two-part spell.
     */
    data class TwoPart(
        override val name: String,
        override val manaCost: Int,
        override val cooldown: Float,
        val secondWord: String
    ) : Spell()

    /**
     * Represents an ultra special spell.
     */
    data class Ultra(
        override val name: String,
        override val manaCost: Int,
        override val cooldown: Float,
        val requiredWords: List<String>
    ) : Spell()
}
