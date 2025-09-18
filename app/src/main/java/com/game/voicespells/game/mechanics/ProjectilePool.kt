package com.game.voicespells.game.mechanics

class ProjectilePool(initial: Int = 32) {
    private val pool = ArrayList<Projectile>(initial)

    init {
        repeat(initial) { pool.add(Projectile()) }
    }

    @Synchronized
    fun obtain(): Projectile {
        val it = pool.firstOrNull { !it.active }
        if (it != null) return it
        val p = Projectile()
        pool.add(p)
        return p
    }

    fun activeProjectiles(): List<Projectile> = pool.filter { it.active }
}
