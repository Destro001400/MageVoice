package com.game.voicespells.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.game.voicespells.utils.Vector3
import io.ktor.client.*
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

object NetworkManager {
    private const val TAG = "NetworkManagerKtor"
    private const val SERVICE_TYPE = "_magevoice._tcp"
    private const val SERVICE_NAME = "MageVoiceHost"
    private const val PORT = 8080

    private var server: ApplicationEngine? = null
    private val client = HttpClient { install(ClientWebSockets) }
    private var clientSession: DefaultClientWebSocketSession? = null

    private val nsdManager by lazy { _context?.getSystemService(Context.NSD_SERVICE) as? NsdManager }
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var _context: Context? = null

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val _gameState = MutableStateFlow(GameState())
    val clientGameState = _gameState.asStateFlow()

    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices = _discoveredServices.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    fun init(context: Context) {
        _context = context.applicationContext
    }

    fun startServer(localPlayerId: String) {
        if (server != null) return
        Log.i(TAG, "Starting server...")

        val hostPlayer = PlayerState(id = localPlayerId, position = Vector3(), velocity = Vector2(0f, 0f), rotation = 0f, hp = 100, mana = 100)
        _gameState.value = GameState(players = mapOf(localPlayerId to hostPlayer))

        server = embeddedServer(CIO, port = PORT) {
            install(ServerWebSockets)
            routing {
                webSocket("/game") {
                    val playerId = call.request.queryParameters["id"] ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Player ID required"))
                    sessions[playerId] = this
                    Log.i(TAG, "Client connected: $playerId")

                    val newPlayer = PlayerState(id = playerId, position = Vector3(), velocity = Vector2(0f, 0f), rotation = 0f, hp = 100, mana = 100)
                    _gameState.update { it.copy(players = it.players + (playerId to newPlayer)) }

                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                handleAction(frame.readText())
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in WebSocket session for $playerId: ${e.message}")
                    } finally {
                        Log.i(TAG, "Client disconnected: $playerId")
                        sessions.remove(playerId)
                        _gameState.update { it.copy(players = it.players - playerId) }
                    }
                }
            }
        }.start(wait = false)

        scope.launch {
            _gameState.collect { state ->
                val stateJson = Json.encodeToString(state)
                sessions.values.forEach { session ->
                    try {
                        session.send(Frame.Text(stateJson))
                    } catch (e: Exception) { /* Ignore */ }
                }
            }
        }
        registerNsdService()
        Log.i(TAG, "Server started on port $PORT")
    }

    private fun handleAction(json: String) {
        try {
            when (val action = Json.decodeFromString<Action>(json)) {
                is UpdatePosition -> {
                    _gameState.update {
                        val updatedPlayers = it.players.toMutableMap()
                        val player = updatedPlayers[action.playerId]
                        if (player != null) {
                            updatedPlayers[action.playerId] = player.copy(velocity = action.velocity)
                        }
                        it.copy(players = updatedPlayers)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding action: $e")
        }
    }

    fun stopServer() {
        Log.i(TAG, "Stopping server...")
        unregisterNsdService()
        server?.stop(1000, 5000)
        server = null
        sessions.clear()
    }

    fun startDiscovery() {
        stopDiscovery()
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) { Log.d(TAG, "Discovery started.") }
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == SERVICE_TYPE) {
                    nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            val currentList = _discoveredServices.value.toMutableList()
                            if (currentList.none { it.serviceName == resolvedService.serviceName }) {
                                currentList.add(resolvedService)
                                _discoveredServices.value = currentList
                            }
                        }
                        override fun onResolveFailed(si: NsdServiceInfo, ec: Int) { Log.e(TAG, "Resolve failed: $ec") }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredServices.value = _discoveredServices.value.filter { it.serviceName != service.serviceName }
            }
            override fun onDiscoveryStopped(st: String) { Log.d(TAG, "Discovery stopped.") }
            override fun onStartDiscoveryFailed(st: String, ec: Int) { Log.e(TAG, "Discovery start failed: $ec") }
            override fun onStopDiscoveryFailed(st: String, ec: Int) { Log.e(TAG, "Discovery stop failed: $ec") }
        }
        nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        if (discoveryListener != null) {
            nsdManager?.stopServiceDiscovery(discoveryListener)
            discoveryListener = null
            _discoveredServices.value = emptyList()
        }
    }

    fun connectToServer(serviceInfo: NsdServiceInfo, localPlayerId: String) {
        scope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = serviceInfo.host.hostAddress, port = PORT, path = "/game?id=$localPlayerId") {
                    clientSession = this
                    Log.i(TAG, "Connected to server!")
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val state = Json.decodeFromString<GameState>(frame.readText())
                            _gameState.value = state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect: ${e.message}")
            } finally {
                Log.i(TAG, "Disconnected.")
                clientSession = null
            }
        }
    }

    fun sendAction(action: Action) {
        scope.launch {
            if (clientSession?.coroutineContext?.get(Job)?.isActive == true) {
                try {
                    clientSession?.send(Frame.Text(Json.encodeToString(action)))
                } catch (e: Exception) { Log.e(TAG, "Failed to send action: ${e.message}") }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            clientSession?.close()
            clientSession = null
        }
    }

    private fun registerNsdService() {
        if (registrationListener != null) return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = PORT
        }
        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(si: NsdServiceInfo) { Log.i(TAG, "NSD service registered") }
            override fun onRegistrationFailed(si: NsdServiceInfo, ec: Int) { Log.e(TAG, "NSD registration failed: $ec") }
            override fun onServiceUnregistered(si: NsdServiceInfo) { Log.i(TAG, "NSD service unregistered") }
            override fun onUnregistrationFailed(si: NsdServiceInfo, ec: Int) { Log.e(TAG, "NSD unregistration failed: $ec") }
        }
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun unregisterNsdService() {
        if (registrationListener != null) {
            nsdManager?.unregisterService(registrationListener)
            registrationListener = null
        }
    }
}