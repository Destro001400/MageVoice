package com.game.voicespells.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.voicespells.game.entities.Player // Assuming Player is serializable or we send PlayerState
import com.game.voicespells.game.spells.Spell // Assuming Spell is serializable or we send SpellCastInfo
import com.game.voicespells.utils.Vector3
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
// For a real implementation, you'd use a robust serialization library like kotlinx.serialization

class NetworkManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private var serviceName: String? = null
    private val SERVICE_TYPE = "_voicespells._tcp." // Example service type
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    // Callbacks for UI updates or game logic integration
    var onServiceRegistered: ((NsdServiceInfo) -> Unit)? = null
    var onServiceFound: ((NsdServiceInfo) -> Unit)? = null
    var onServiceLost: ((NsdServiceInfo) -> Unit)? = null
    var onGameHosted: ((String, Int) -> Unit)? = null // IP, Port
    var onGameJoined: ((InetAddress, Int) -> Unit)? = null
    var onGameStateReceived: ((GameState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    companion object {
        const val TAG = "NetworkManager"
    }

    fun hostGame(roomName: String) {
        try {
            // Initialize ServerSocket to an available port
            serverSocket = ServerSocket(0) // 0 for a random available port
            val localPort = serverSocket!!.localPort

            registerService(localPort, roomName)
            this.serviceName = roomName
            // Start listening for client connections in a new thread
            Thread {
                try {
                    Log.d(TAG, "Server listening on port $localPort")
                    // For now, we just log. Actual game would accept connections.
                    // val client = serverSocket?.accept()
                    // handleClientConnection(client)
                    mainHandler.post { onGameHosted("localhost", localPort) }
                } catch (e: IOException) {
                    Log.e(TAG, "ServerSocket accept failed or closed", e)
                    mainHandler.post { onError?.invoke("Failed to host game: ${e.message}") }
                }
            }.start()

        } catch (e: IOException) {
            Log.e(TAG, "Could not initialize ServerSocket", e)
            mainHandler.post { onError?.invoke("Failed to initialize host: ${e.message}") }
        }
    }

    private fun registerService(port: Int, roomName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = roomName // Should be unique on the network for the service type
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                this@NetworkManager.serviceName = nsdServiceInfo.serviceName
                Log.d(TAG, "Service registered: $nsdServiceInfo")
                mainHandler.post { onServiceRegistered?.invoke(nsdServiceInfo) }
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed. Error code: $errorCode")
                mainHandler.post { onError?.invoke("Service registration failed: $errorCode") }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed. Error code: $errorCode")
            }
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        stopDiscovery() // Stop any previous discovery
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: $service")
                // Resolve the service to get IP and port if it's our game type
                if (service.serviceType == SERVICE_TYPE) {
                    // Don't resolve your own service
                    if (service.serviceName != serviceName) {
                        resolveService(service)
                    }
                } else {
                    Log.d(TAG, "Found service of different type: ${service.serviceType}")
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "Service lost: $service")
                mainHandler.post { onServiceLost?.invoke(service) }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed. Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
                mainHandler.post { onError?.invoke("Discovery start failed: $errorCode") }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed. Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for $serviceInfo. Error code: $errorCode")
                 mainHandler.post { onError?.invoke("Resolve failed: $errorCode") }
            }

            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: $resolvedServiceInfo")
                // Now you have the host IP and port
                mainHandler.post { onServiceFound?.invoke(resolvedServiceInfo) }
                // You might automatically call joinGame here or let UI decide
            }
        }
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    fun joinGame(serviceInfo: NsdServiceInfo) {
        // Use resolvedServiceInfo.host and resolvedServiceInfo.port to connect
        val hostAddress = serviceInfo.host
        val port = serviceInfo.port
        if (hostAddress == null) {
            Log.e(TAG, "Cannot join game, host address is null for $serviceInfo")
            mainHandler.post{ onError?.invoke("Cannot join game: Host address is null") }
            return
        }

        Thread {
            try {
                Log.d(TAG, "Attempting to connect to $hostAddress:$port")
                clientSocket = Socket(hostAddress, port)
                Log.d(TAG, "Successfully connected to game at $hostAddress:$port")
                mainHandler.post { onGameJoined?.invoke(hostAddress, port) }
                // Start listening for game state updates from server
                // listenForGameStateUpdates()
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect to game at $hostAddress:$port", e)
                 mainHandler.post{ onError?.invoke("Join game failed: ${e.message}") }
            }
        }.start()
    }
    
    // Overload for direct IP connection (e.g. if NSD is not used or user inputs IP manually)
    fun joinGame(ipAddress: String, port: Int) {
         Thread {
            try {
                val hostAddress = InetAddress.getByName(ipAddress)
                Log.d(TAG, "Attempting to connect to $hostAddress:$port")
                clientSocket = Socket(hostAddress, port)
                Log.d(TAG, "Successfully connected to game at $hostAddress:$port")
                mainHandler.post { onGameJoined?.invoke(hostAddress, port) }
                // listenForGameStateUpdates()
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect to game at $ipAddress:$port", e)
                 mainHandler.post{ onError?.invoke("Join game failed: ${e.message}") }
            }
        }.start()
    }


    fun sendPlayerState(player: Player) {
        // This would serialize the Player object (or a simplified PlayerState)
        // and send it over the clientSocket or to all clients if this is the host.
        // Example: val playerState = PlayerState(player.id, player.position, player.rotation, player.hp, player.mana)
        // val serializedData = serialize(playerState) // Using a serialization library
        // outputStream?.write(serializedData)
        if (clientSocket?.isConnected == true || serverSocket != null) {
             // Log.d(TAG, "Sending Player State for ${player.id}: Pos=${player.position}, HP=${player.hp}")
        } // Actual sending logic needed
    }

    fun sendSpellCast(spell: Spell, position: Vector3, casterId: String) {
        // Similar to sendPlayerState, serialize SpellCastInfo and send.
        // Example: val spellCastInfo = SpellCastInfo(spell.name, casterId, position)
        // val serializedData = serialize(spellCastInfo)
        // outputStream?.write(serializedData)
        if (clientSocket?.isConnected == true || serverSocket != null) {
            // Log.d(TAG, "Sending Spell Cast: ${spell.name} by $casterId at $position")
        } // Actual sending logic needed
    }

    fun receiveGameState(): GameState {
        // This method implies a blocking call or a way to retrieve latest state.
        // In a real-time game, you'd likely have a listener for incoming data
        // that continuously updates the local game state.
        // For now, this is a placeholder.
        // Example: val data = inputStream?.read()
        // val gameState = deserialize(data) as GameState
        // return gameState
        Log.d(TAG, "receiveGameState() called - placeholder")
        return GameState() // Return an empty/default state for now
    }
    
    // private fun listenForGameStateUpdates() {
    //    Thread {
    //        try {
    //            val inputStream = clientSocket?.getInputStream() // Or from accepted server connections
    //            while(clientSocket?.isConnected == true || serverSocket?.isClosed == false) {
    //                 // Read data, deserialize to GameState or individual updates
    //                 // val gameState = deserializeAndProcess(inputStream)
    //                 // mainHandler.post { onGameStateReceived?.invoke(gameState) }
    //            }
    //        } catch (e: IOException) {
    //            Log.e(TAG, "Error listening for game state updates", e)
    //        }
    //    }.start()
    // }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: IllegalArgumentException) {
                 Log.w(TAG, "Error stopping discovery, listener not registered?", e)
            }
            discoveryListener = null
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Error unregistering service, listener not registered?", e)
            }
            registrationListener = null
        }
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serviceName = null
    }

    fun cleanup() {
        stopDiscovery()
        unregisterService()
        try {
            clientSocket?.close()
            clientSocket = null
        } catch (e: IOException) {
            Log.e(TAG, "Error closing client socket", e)
        }
        Log.d(TAG, "NetworkManager cleaned up.")
    }
}
