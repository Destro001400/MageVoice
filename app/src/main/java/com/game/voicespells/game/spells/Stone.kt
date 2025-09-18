package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import android.util.Log

class Stone : Spell() {
    override val name: String = "Stone Shield"
    override val manaCost: Int = 15
    override val damage: Int = 0
    override val cooldown: Float = 10.0f
    override val voiceCommand: String = "stone"

    val shieldAmount: Int = 50

    override fun execute(caster: Player, target: Vector3) {
        Log.d("Spell", "${caster.id} casts Stone Shield: grant $shieldAmount HP (simulated)")
        caster.hp += shieldAmount
        if (caster.hp > 200) caster.hp = 200
    }
}
