package com.game.voicespells.game.mechanics

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import com.game.voicespells.game.spells.Spell
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

class GameWorld {
    val players = CopyOnWriteArrayList<Player>()
    val projectilePool = ProjectilePool()
    private val spells = CopyOnWriteArrayList<Pair<Spell, Vector3>>()
    private var running = false

    fun start() {
        running = true
        thread {
            var last = System.currentTimeMillis()
            while (running) {
                val now = System.currentTimeMillis()
                val dt = now - last
                update(dt)
                last = now
                Thread.sleep(16)
            }
        }
    }

    fun stop() {
        running = false
    }

    private fun update(dtMs: Long) {
        // update projectiles
        projectilePool.activeProjectiles().forEach { it.update(dtMs) }

        // stub: check collisions and apply damage
        // update players regen
        players.forEach { p ->
            if (p.hp < 100) p.hp += 1
            if (p.mana < 100) p.mana += 1
        }
    }

    fun spawnProjectile(pos: Vector3, vel: Vector3, damage: Int, ownerId: String) {
        val p = projectilePool.obtain()
        p.reset(pos, vel, damage, ownerId)
    }

    fun dispatchSpellVisual(spell: Spell, pos: Vector3) {
        spells.add(Pair(spell, pos))
    }
}
