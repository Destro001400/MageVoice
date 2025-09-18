package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import android.util.Log

class Gust : Spell() {
    override val name: String = "Gust"
    override val manaCost: Int = 10
    override val damage: Int = 0 // Gust is primarily for knockback
    override val cooldown: Float = 4.0f // seconds
    override val voiceCommand: String = "gust"

    val knockbackForce: Float = 5.0f
    val areaOfEffectRadius: Float = 4.0f

    override fun execute(caster: Player, target: Vector3) {
        Log.d("Spell", "${caster.id} casts Gust at $target: push effect")
        // Stub: push nearby players away
    }
}
