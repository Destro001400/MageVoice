package com.game.voicespells.game.spells

import com.game.voicespells.game.entities.Player
import com.game.voicespells.utils.Vector3
import com.game.voicespells.game.mechanics.GameSingleton

class Fireball : Spell() {
    override val name: String = "Fireball"
    override val manaCost: Int = 20
    override val damage: Int = 30
    override val cooldown: Float = 2.0f // seconds
    override val voiceCommand: String = "fireball"

    override fun execute(caster: Player, target: Vector3) {
        // compute direction and velocity
        val t = target
    val dirX = t.x - caster.x
    val dirY = t.y - caster.y
    val len = kotlin.math.sqrt((dirX * dirX + dirY * dirY).toDouble()).toFloat()
        val speed = 600f // px per second
        val vx = if (len == 0f) speed else dirX / len * speed
        val vy = if (len == 0f) 0f else dirY / len * speed

        GameSingleton.world.spawnProjectile(Vector3(caster.x, caster.y, 0f), Vector3(vx, vy, 0f), damage, caster.id)
    }
}
