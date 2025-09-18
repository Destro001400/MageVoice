package com.game.voicespells.core.network

import java.util.concurrent.ConcurrentHashMap

class GameSyncManager {
    data class PlayerState(val id: String, val x: Float, val y: Float, val z: Float, val hp: Int, val mana: Int)
    data class SpellEvent(val casterId: String, val spellName: String, val targetX: Float, val targetY: Float, val targetZ: Float)

    private val playerStates = ConcurrentHashMap<String, PlayerState>()
    private val spellEvents = mutableListOf<SpellEvent>()
    private val maxSpellEvents = 50

    fun updatePlayerState(state: PlayerState) {
        playerStates[state.id] = state
        // Remove estados antigos se necessário (exemplo: manter até 100 jogadores)
        if (playerStates.size > 100) {
            val oldest = playerStates.keys.firstOrNull()
            if (oldest != null) playerStates.remove(oldest)
        }
    }

    fun getPlayerStates(): List<PlayerState> = playerStates.values.toList()

    fun addSpellEvent(event: SpellEvent) {
        spellEvents.add(event)
        if (spellEvents.size > maxSpellEvents) {
            spellEvents.removeAt(0)
        }
    }
    fun getPlayerStatesCompressed(): ByteArray {
        val json = getPlayerStates().toString()
        return java.util.zip.Deflater().run {
            setInput(json.toByteArray())
            finish()
            val out = ByteArray(4096)
            val len = deflate(out)
            out.copyOf(len)
        }
    }

    fun getSpellEventsCompressed(): ByteArray {
        val json = getSpellEvents().toString()
        return java.util.zip.Deflater().run {
            setInput(json.toByteArray())
            finish()
            val out = ByteArray(4096)
            val len = deflate(out)
            out.copyOf(len)
        }
    }

    fun getSpellEvents(): List<SpellEvent> = spellEvents.toList()

    fun clearSpellEvents() {
        spellEvents.clear()
    }
}
