package com.game.voicespells.domain.entities

data class PlayerState(
    val id: Float,
    var x: Float,
    var y: Float
) {
    fun toFloatArray(): FloatArray = floatArrayOf(id, x, y)
}

data class ProjectileState(
    val id: Float,
    var x: Float,
    var y: Float,
    val type: Float // e.g., 0 for fireball, 1 for ice, etc.
) {
    fun toFloatArray(): FloatArray = floatArrayOf(id, x, y, type)
}

data class GameState(
    val players: List<PlayerState> = emptyList(),
    val projectiles: List<ProjectileState> = emptyList()
) {
    fun playersToFloatArray(): FloatArray {
        return players.flatMap { it.toFloatArray().asIterable() }.toFloatArray()
    }

    fun projectilesToFloatArray(): FloatArray {
        return projectiles.flatMap { it.toFloatArray().asIterable() }.toFloatArray()
    }
}
