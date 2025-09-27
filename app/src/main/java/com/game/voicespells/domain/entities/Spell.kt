package com.game.voicespells.domain.entities

// As per the GDD, representing different spell categories.
// Using a sealed class allows for a restricted hierarchy, which is great for exhaustive checks.
sealed class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val cooldown: Float
    // The `cast` function is commented out as it requires Player and Vector3, which are not defined yet.
    // abstract fun cast(caster: Player, target: Vector3)
}

/**
 * Represents the 5 basic spells that are always unlocked.
 * They are activated by a single word.
 */
sealed class BasicSpell : Spell() {
    override val cooldown: Float get() = 0.5f // Global cooldown as per GDD
}

// Placeholder classes for future implementation based on the GDD
sealed class AdvancedSpell : Spell()
sealed class SpecialSpell : Spell()
sealed class TwoPartSpell : Spell()
sealed class UltraSpell : Spell()

// --- Implementation of the 5 Basic Spells for the MVP ---

object Fireball : BasicSpell() {
    override val name: String = "Fireball"
    override val manaCost: Int = 15 // Example value
    // Properties: Projétil direto, 30 dano
}

object Freezing : BasicSpell() {
    override val name: String = "Freezing"
    override val manaCost: Int = 20 // Example value
    // Properties: AoE 3m, slow 50% por 3s
}

object Lightning : BasicSpell() {
    override val name: String = "Lightning"
    override val manaCost: Int = 25 // Example value
    // Properties: Instantâneo, 25 dano, stun 0.5s
}

object Stone : BasicSpell() {
    override val name: String = "Stone"
    override val manaCost: Int = 10 // Example value
    // Properties: Escudo temporário, 50 HP por 5s
}

object Gust : BasicSpell() {
    override val name: String = "Gust"
    override val manaCost: Int = 10 // Example value
    // Properties: Empurrão AoE, 5m de knockback
}
