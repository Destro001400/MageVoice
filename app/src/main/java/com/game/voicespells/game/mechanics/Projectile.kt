package com.game.voicespells.game.mechanics

import com.game.voicespells.utils.Vector3

data class Projectile(
    var position: Vector3 = Vector3(),
    var velocity: Vector3 = Vector3(),
    var damage: Int = 0,
    var ownerId: String = "",
    var ttlMs: Long = 3000L,
    var active: Boolean = false
) {
    private var ageMs: Long = 0

    fun reset(pos: Vector3, vel: Vector3, dmg: Int, owner: String, ttl: Long = 3000L) {
        position = pos
        velocity = vel
        damage = dmg
        ownerId = owner
        ttlMs = ttl
        ageMs = 0
        active = true
    }

    fun update(dtMs: Long) {
        if (!active) return
        // simple Euler integration
        val dt = dtMs / 1000f
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        position.z += velocity.z * dt
        ageMs += dtMs
        if (ageMs >= ttlMs) active = false
    }
}
