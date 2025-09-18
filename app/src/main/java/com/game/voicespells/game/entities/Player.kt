package com.game.voicespells.game.entities

import com.game.voicespells.game.spells.Spell
import com.game.voicespells.utils.Vector3
import com.game.voicespells.utils.GameConfig
import kotlin.concurrent.schedule
import java.util.Timer

/**
 * Represents a player in the game.
 */
data class Player(
    var position: Vector3,
    var rotation: Float, // rotation in radians or degrees depending on usage
    var hp: Int = 100,
    var mana: Int = 100,
    var selectedSpells: List<Spell> = emptyList(),
    val id: String
) {
    private var alive = true

    // Helper properties to match existing code that used caster.x / caster.y
    var x: Float
        get() = position.x
        set(value) { position.x = value }

    var y: Float
        get() = position.y
        set(value) { position.y = value }

    fun takeDamage(amount: Int) {
        if (!alive) return
        hp -= amount
        if (hp <= 0) {
            hp = 0
            alive = false
            respawn()
        }
    }

    fun useMana(amount: Int): Boolean {
        if (mana >= amount) {
            mana -= amount
            return true
        }
        return false
    }

    fun castSpell(spell: Spell, target: Vector3) {
        if (!alive) return
        if (!useMana(spell.manaCost)) return
        spell.execute(this, target)
    }

    fun respawn() {
        // simple respawn after RESPAWN_TIME seconds
        Timer().schedule((GameConfig.RESPAWN_TIME * 1000).toLong()) {
            hp = 100
            mana = 100
            alive = true
            position = Vector3(0f, 0f, 0f)
        }
    }
}
