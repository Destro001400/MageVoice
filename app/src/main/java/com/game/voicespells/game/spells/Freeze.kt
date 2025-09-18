package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import android.util.Log

class Freeze : Spell() {
    override val name: String = "Freeze"
    override val manaCost: Int = 25
    override val damage: Int = 0 // Freeze is a utility spell, direct damage is 0
    override val cooldown: Float = 5.0f // seconds
    override val voiceCommand: String = "freeze"

    // Spell-specific properties
    val areaOfEffectRadius: Float = 3.0f
    val slowFactor: Float = 0.5f // 50% slow
    val slowDuration: Float = 3.0f // 3 seconds

    override fun execute(caster: Player, target: Vector3) {
        Log.d("Spell", "${caster.id} casts Freeze at $target. (AOE slow)")
        // Stub: apply slow to nearby players within areaOfEffectRadius
    }
}
