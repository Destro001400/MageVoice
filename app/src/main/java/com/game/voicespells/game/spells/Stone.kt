package com.game.voicespells.game.spells

import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3

class Stone : Spell() {
    override val name: String = "Stone Shield" // Changed name to be more descriptive
    override val manaCost: Int = 15
    override val damage: Int = 0 // Stone is a defensive spell
    override val cooldown: Float = 10.0f // Example cooldown
    override val voiceCommand: String = "stone"
    override val spellType: SpellType = SpellType.STONE

    private val temporaryHpGain: Int = 50
    private val shieldDuration: Float = 8.0f // seconds

    override fun execute(
        caster: Player,
        targetPosition: Vector3, // Not directly used for a self-cast shield, could be caster's position
        playersInScene: List<Player> // Not used for a self-cast shield
    ) {
        if (caster.mana >= manaCost) {
            caster.useMana(manaCost)
            Log.d("Stone", "${caster.id} casts Stone Shield!")

            // Apply temporary HP to the caster
            // In a real game, this would be a temporary status effect or a separate "shield HP" pool.
            // For simplicity, we'll add it directly and assume it wears off.
            // A more robust system would involve a StatusEffectManager.
            caster.applyTemporaryHp(temporaryHpGain, shieldDuration)

            Log.d("Stone", "${caster.id} gains $temporaryHpGain temporary HP for $shieldDuration seconds.")

        } else {
            Log.d("Stone", "${caster.id} tried to cast Stone Shield but not enough mana.")
        }
    }
}
