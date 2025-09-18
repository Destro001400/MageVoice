package com.game.voicespells.utils

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun distanceTo(other: Vector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }
}
