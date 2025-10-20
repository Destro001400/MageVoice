package com.game.voicespells.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Vector2(val x: Float, val y: Float)

@Serializable
sealed class Action

@Serializable
@SerialName("update_pos")
data class UpdatePosition(val playerId: String, val velocity: Vector2) : Action()