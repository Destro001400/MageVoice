package com.game.voicespells.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.spells.Spell
import com.game.voicespells.utils.Vector3
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

// Simplified NetworkManager: a single coherent implementation for hosting/discovery placeholders.
class NetworkManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private var serviceName: String? = null
    private val SERVICE_TYPE = "_voicespells._tcp." // Example service type
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        const val TAG = "NetworkManager"
    }

    fun hostGame(roomName: String) {
        try {
            serverSocket = ServerSocket(0)
            val localPort = serverSocket!!.localPort
            registerService(localPort, roomName)
            this.serviceName = roomName
            Thread {
                try {
                    Log.d(TAG, "Server listening on port $localPort")
                    mainHandler.post { /* callback to UI if needed */ }
                } catch (e: IOException) {
                    Log.e(TAG, "ServerSocket accept failed or closed", e)
                }
            }.start()
        } catch (e: IOException) {
            Log.e(TAG, "Could not initialize ServerSocket", e)
        }
    }

    private fun registerService(port: Int, roomName: String) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = roomName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }
        try {
            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    serviceName = nsdServiceInfo.serviceName
                    Log.d(TAG, "Service registered: $nsdServiceInfo")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            })
        } catch (e: Exception) {
            Log.w(TAG, "registerService failed", e)
        }
    }

    fun joinGame(ipAddress: String, port: Int) {
        Thread {
            try {
                val hostAddress = InetAddress.getByName(ipAddress)
                clientSocket = Socket(hostAddress, port)
                Log.d(TAG, "Connected to $hostAddress:$port")
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect to game at $ipAddress:$port", e)
            }
        }.start()
    }

    fun sendPlayerState(player: Player) {
        // Placeholder for sending player state over clientSocket or server
        if (clientSocket?.isConnected == true) {
            // send serialized data
        }
    }

    fun sendSpellCast(spell: Spell, position: Vector3, casterId: String) {
        if (clientSocket?.isConnected == true) {
            // send serialized spell cast
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery", e)
        }
        discoveryListener = null
    }

    fun unregisterService() {
        registrationListener?.let { try { nsdManager?.unregisterService(it) } catch (e: Exception) { Log.w(TAG, "unregister failed", e) } }
        registrationListener = null
        try { serverSocket?.close() } catch (e: Exception) { Log.w(TAG, "close serverSocket", e) }
        serverSocket = null
    }

    fun cleanup() {
        stopDiscovery()
        unregisterService()
        try { clientSocket?.close() } catch (e: Exception) { Log.w(TAG, "close clientSocket", e) }
        clientSocket = null
    }
}
