package com.game.voicespells.core.network

import android.content.Context
import java.util.UUID

class LanRoomManager(private val context: Context) {
    data class Room(val id: String, val name: String, val players: MutableList<String>)

    private val rooms = mutableListOf<Room>()
    private var currentRoom: Room? = null

    fun createRoom(callback: (Boolean) -> Unit) {
        // Simula criação de sala
        val room = Room(UUID.randomUUID().toString(), "Sala ${rooms.size + 1}", mutableListOf())
        rooms.add(room)
        currentRoom = room
        callback(true)
    }

    fun joinRoom(roomId: String, callback: (Boolean) -> Unit) {
        val room = rooms.find { it.id == roomId }
        if (room != null) {
            // Simula entrada do jogador
            room.players.add("Você")
            currentRoom = room
            callback(true)
        } else {
            callback(false)
        }
    }

    fun leaveRoom(playerId: String) {
        currentRoom?.players?.remove(playerId)
        if (currentRoom?.players?.isEmpty() == true) {
            rooms.remove(currentRoom)
            currentRoom = null
        }
    }

    fun getAvailableRooms(callback: (List<Room>) -> Unit) {
        // Simula busca de salas
        callback(rooms)
    }
    fun getCurrentRoom(): Room? = currentRoom
}
