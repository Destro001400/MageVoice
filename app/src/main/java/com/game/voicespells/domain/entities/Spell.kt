package com.game.voicespells.domain.entities

sealed class Spell {
    abstract val name: String
    abstract val manaCost: Int
    abstract val cooldown: Float
    abstract val activationKeyword: String
}

data class BasicSpell(
    override val name: String,
    override val manaCost: Int,
    override val cooldown: Float,
    override val activationKeyword: String,
    val damage: Int
) : Spell()

data class AdvancedSpell(
    override val name: String,
    override val manaCost: Int,
    override val cooldown: Float,
    override val activationKeyword: String
) : Spell()

data class SpecialSpell(
    override val name: String,
    override val manaCost: Int,
    override val cooldown: Float,
    override val activationKeyword: String
) : Spell()

data class TwoPartSpell(
    override val name: String,
    override val manaCost: Int,
    override val cooldown: Float,
    override val activationKeyword: String,
    val secondKeyword: String
) : Spell()

data class UltraSpell(
    override val name: String,
    override val manaCost: Int,
    override val cooldown: Float,
    override val activationKeyword: String,
    val secondKeyword: String,
    val thirdKeyword: String
) : Spell()