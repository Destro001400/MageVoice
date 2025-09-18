package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import android.util.Log

class Lightning : Spell() {
    override val name: String = "Lightning"
    override val manaCost: Int = 30
    override val damage: Int = 25
    override val cooldown: Float = 3.0f
    override val voiceCommand: String = "lightning"

    override fun execute(caster: Player, target: Vector3) {
        Log.d("Spell", "${caster.id} casts Lightning at $target for $damage damage")
        // instant hit logic (stub)
    }
}
