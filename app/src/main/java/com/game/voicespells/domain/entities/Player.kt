package com.game.voicespells.domain.entities

/**
 * Represents a player in the game.
 * Based on the GDD section "Parâmetros do Jogador".
 */
data class Player(
    val id: String,
    var hp: Int = 100,
    var mana: Int = 100,
    val speed: Float = 5.0f, // meters per second
    // Position will be handled in the game engine/view layer, but we can have a representation here if needed.
    // var position: Vector3 = Vector3.ZERO
) {
    companion object {
        const val MAX_HP = 100
        const val MAX_MANA = 100
        const val RESPAWN_TIME_SECONDS = 10
    }

    fun takeDamage(amount: Int) {
        hp = (hp - amount).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        hp = (hp + amount).coerceAtMost(MAX_HP)
    }

    fun useMana(amount: Int): Boolean {
        return if (mana >= amount) {
            mana -= amount
            true
        } else {
            false
        }
    }

    fun regenerateMana(amount: Int) {
        mana = (mana + amount).coerceAtMost(MAX_MANA)
    }
}