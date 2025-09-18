package com.game.voicespells.game.mechanics

import com.game.voicespells.game.spells.*
import com.game.voicespells.voice.SpellType
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

class SpellSystem {
    private val spellsByType = mapOf(
        SpellType.FIREBALL to Fireball(),
        SpellType.FREEZE to Freeze(),
        SpellType.LIGHTNING to Lightning(),
        SpellType.STONE to Stone(),
        SpellType.GUST to Gust()
    )

    private val cooldowns = ConcurrentHashMap<String, Long>()

    fun getSpell(type: SpellType): Spell? = spellsByType[type]

    fun tryCast(caster: Player, type: SpellType, target: Vector3) {
        val spell = getSpell(type) ?: return
        val now = System.currentTimeMillis()
        val key = caster.id + ":" + spell.name
        val last = cooldowns[key] ?: 0L
        if (now - last < (spell.cooldown * 1000).toLong()) {
            Log.d("SpellSystem", "Spell on cooldown: ${spell.name}")
            return
        }
        if (!caster.useMana(spell.manaCost)) {
            Log.d("SpellSystem", "Not enough mana for ${spell.name}")
            return
        }
        cooldowns[key] = now
        spell.execute(caster, target)
        Log.d("SpellSystem", "${caster.id} cast ${spell.name}")
    }
}
