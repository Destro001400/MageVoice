package com.game.voicespells.data.network

import android.content.Context
import android.net.wifi.WifiManager
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NetworkManager(private val context: Context) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private var server: CIOApplicationEngine? = null
    private val clientSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    private val _connectionEvents = MutableSharedFlow<ConnectionEvent>()
    val connectionEvents = _connectionEvents.asSharedFlow()

    // ... (Discovery properties) ...

    fun startServer() {
        server = embeddedServer(CIO, port = 8080) {
            install(WebSockets)
            routing {
                webSocket("/ws") {
                    val clientId = UUID.randomUUID().toString()
                    clientSessions[clientId] = this
                    coroutineScope.launch { _connectionEvents.emit(ConnectionEvent.ClientConnected(clientId)) }
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val event = Json.decodeFromString<GameEvent>(text)
                                coroutineScope.launch { _connectionEvents.emit(ConnectionEvent.EventReceived(clientId, event)) }
                            }
                        }
                    } finally {
                        clientSessions.remove(clientId)
                        coroutineScope.launch { _connectionEvents.emit(ConnectionEvent.ClientDisconnected(clientId)) }
                    }
                }
            }
        }.start(wait = false)
        startHostDiscovery()
    }

    fun broadcast(event: GameEvent) {
        coroutineScope.launch {
            val message = Json.encodeToString(event)
            clientSessions.values.forEach { it.send(Frame.Text(message)) }
        }
    }

    // ... (Client implementation) ...
    fun startClient(hostIp: String) {
        coroutineScope.launch {
            HttpClient { install(WebSockets) }.webSocket(host = hostIp, port = 8080, path = "/ws") {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val event = Json.decodeFromString<GameEvent>(text)
                        coroutineScope.launch { _connectionEvents.emit(ConnectionEvent.EventReceived("server", event)) }
                    }
                }
            }
        }
    }

    // ... (Other methods: startHostDiscovery, findHost, sendToServer, stop) ...
}