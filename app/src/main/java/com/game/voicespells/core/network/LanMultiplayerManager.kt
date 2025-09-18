package com.game.voicespells.core.network

import android.content.Context
import com.game.voicespells.core.network.LanRoomManager
import com.game.voicespells.core.network.WebRtcVoiceManager
import com.game.voicespells.core.network.GameSyncManager
import com.game.voicespells.core.network.LanUdpSyncService
import com.game.voicespells.core.network.SyncJsonUtil

class LanMultiplayerManager(private val context: Context) {
    val roomManager = LanRoomManager(context)
    val voiceManager = WebRtcVoiceManager(context)
    val syncManager = GameSyncManager()
    private val udpSyncService = LanUdpSyncService(context)

    init {
        udpSyncService.onMessageReceived = { msg ->
            // Mensagem pode ser do tipo "state" ou "spell"
            if (msg.startsWith("state:")) {
                val json = msg.removePrefix("state:")
                val states = SyncJsonUtil.playerStatesFromJson(json)
                states.forEach { syncManager.updatePlayerState(it) }
            } else if (msg.startsWith("spell:")) {
                val json = msg.removePrefix("spell:")
                val spells = SyncJsonUtil.spellEventsFromJson(json)
                spells.forEach { syncManager.addSpellEvent(it) }
            }
        }
        udpSyncService.start()
    }

    fun createRoom(name: String): LanRoomManager.Room {
        return roomManager.createRoom(name)
    }

    fun joinRoom(roomId: String, playerId: String): Boolean {
        return roomManager.joinRoom(roomId, playerId)
    }

    fun leaveRoom(playerId: String) {
        roomManager.leaveRoom(playerId)
    }

    fun startVoice() {
        voiceManager.initialize()
        voiceManager.startAudioStream()
    }

    fun stopVoice() {
        voiceManager.stopAudioStream()
        voiceManager.close()
    }

    fun updatePlayerState(state: GameSyncManager.PlayerState) {
        syncManager.updatePlayerState(state)
        val json = SyncJsonUtil.playerStatesToJson(listOf(state))
        udpSyncService.send("state:$json")
    }

    fun addSpellEvent(event: GameSyncManager.SpellEvent) {
        syncManager.addSpellEvent(event)
        val json = SyncJsonUtil.spellEventsToJson(listOf(event))
        udpSyncService.send("spell:$json")
    }

    fun stopSync() {
        udpSyncService.stop()
    }

    fun getPlayerStates(): List<GameSyncManager.PlayerState> = syncManager.getPlayerStates()
    fun getSpellEvents(): List<GameSyncManager.SpellEvent> = syncManager.getSpellEvents()
    fun clearSpellEvents() = syncManager.clearSpellEvents()
}
