package com.game.voicespells.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.game.voicespells.game.entities.Player
import com.game.voicespells.game.entities.Vector3
import com.game.voicespells.game.spells.SpellType
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
// Para UDP e serialização de dados, precisaremos de mais importações depois (ex: Ktor, Kotlinx Serialization)

data class GameStateUpdate(
    val playerId: String,
    val position: Vector3,
    val rotation: Float,
    val hp: Int,
    val mana: Int
    // Add other relevant fields like velocity, current action, etc.
)

data class SpellCastAction(
    val casterId: String,
    val spellType: SpellType,
    val targetPosition: Vector3
    // Potencialmente, ID do alvo se houver
)

// Outras classes de mensagens de rede podem ser definidas aqui

class NetworkManager(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private val SERVICE_TYPE = "_magevoice._tcp" // Formato: _<protocolo>._<transporte>
    private val SERVICE_NAME_PREFIX = "MageVoiceHost"
    private var hostServiceName: String? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null // Para o cliente se conectar ao host
    private val connectedClients = mutableListOf<Socket>() // Para o host gerenciar múltiplos clientes

    var onPlayerDiscovered: ((Player) -> Unit)? = null // Placeholder, precisa de mais info
    var onPlayerDisconnected: ((String) -> Unit)? = null
    var onGameStateReceived: ((GameStateUpdate) -> Unit)? = null
    var onSpellCastReceived: ((SpellCastAction) -> Unit)? = null
    var onConnectedAsHost: (() -> Unit)? = null
    var onConnectedAsClient: ((InetAddress, Int) -> Unit)? = null
    var onConnectionFailed: ((String) -> Unit)? = null
    var onDiscoveryStopped: (() -> Unit)? = null
    var onServiceRegistered: ((String) -> Unit)? = null


    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "NetworkManager"
    private var currentPort: Int = -1

    // --- Servidor / Host ---
    fun startHost(playerName: String) {
        try {
            // Escolher uma porta disponível
            serverSocket = ServerSocket(0).also { currentPort = it.localPort }
            Log.d(TAG, "Host started on port $currentPort")
            hostServiceName = "$SERVICE_NAME_PREFIX-$playerName-${System.currentTimeMillis() % 10000}"
            registerService(currentPort, hostServiceName!!)

            // Iniciar thread para aceitar conexões de clientes
            Thread {
                try {
                    while (!serverSocket!!.isClosed) {
                        val client = serverSocket!!.accept()
                        Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")
                        connectedClients.add(client)
                        // TODO: Iniciar thread para lidar com este cliente (receber dados)
                        // TODO: Enviar informações iniciais do jogo para este cliente
                        // TODO: Informar outros clientes sobre o novo jogador
                    }
                } catch (e: IOException) {
                    if (serverSocket?.isClosed == false) {
                        Log.e(TAG, "ServerSocket accept error: ${e.message}", e)
                    } else {
                        Log.d(TAG, "ServerSocket closed, accept loop ended.")
                    }
                }
            }.start()
            handler.post { onConnectedAsHost?.invoke() }

        } catch (e: IOException) {
            Log.e(TAG, "Could not start host: ${e.message}", e)
            handler.post { onConnectionFailed?.invoke("Falha ao iniciar host: ${e.message}") }
            cleanupHost()
        }
    }


    // --- Cliente ---
    fun discoverHosts() {
        if (discoveryListener != null) {
            Log.d(TAG, "Discovery already active or not cleaned up. Stopping previous.")
            stopDiscovery() // Garante que a descoberta anterior seja interrompida
        }
        initializeDiscoveryListener() // Reinicializa para evitar problemas de estado
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        Log.d(TAG, "Starting service discovery for $SERVICE_TYPE")
    }

    fun connectToHost(serviceInfo: NsdServiceInfo) {
        Log.d(TAG, "Attempting to connect to host: ${serviceInfo.serviceName} at ${serviceInfo.host}:${serviceInfo.port}")
        Thread {
            try {
                clientSocket = Socket(serviceInfo.host, serviceInfo.port)
                Log.d(TAG, "Connected to host: ${serviceInfo.hostName} on port ${serviceInfo.port}")
                // TODO: Iniciar thread para receber dados do host
                // TODO: Enviar informações do jogador local para o host
                handler.post { onConnectedAsClient?.invoke(serviceInfo.host, serviceInfo.port) }
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect to host: ${e.message}", e)
                handler.post { onConnectionFailed?.invoke("Falha ao conectar ao host: ${e.message}") }
                clientSocket?.close()
                clientSocket = null
            }
        }.start()
    }

    // --- NSD (Network Service Discovery) ---
    private fun registerService(port: Int, serviceName: String) {
        if (registrationListener != null) {
            Log.w(TAG, "Service already registered or listener not null. Attempting to unregister first.")
            // Tentar cancelar registro anterior pode ser problemático se o listener estiver em uso
            // Idealmente, garantir que cleanupHost/stopDiscovery limpem o listener.
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) { Log.w(TAG, "Error unregistering previous service during register: ${e.message}") }
            registrationListener = null // Forçar reinicialização
        }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                this@NetworkManager.hostServiceName = NsdServiceInfo.serviceName
                Log.d(TAG, "Service registered: ${NsdServiceInfo.serviceName}")
                handler.post{ onServiceRegistered?.invoke(NsdServiceInfo.serviceName) }
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed. Error code: $errorCode")
                handler.post { onConnectionFailed?.invoke("Falha ao registrar serviço NSD: $errorCode") }
                cleanupHost() // Limpar se o registro falhar
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${arg0.serviceName}")
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed. Error code: $errorCode")
            }
        }
        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during registerService: ${e.message}", e)
            handler.post { onConnectionFailed?.invoke("Exceção ao registrar serviço: ${e.message}") }
        }
    }

    private fun initializeDiscoveryListener() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}, type: ${service.serviceType}")
                if (service.serviceType == SERVICE_TYPE && service.serviceName != hostServiceName) {
                    // Serviço é do tipo correto e não é o próprio host
                    // Precisamos resolver o serviço para obter IP e porta
                    if (resolveListener != null) {
                        // Evitar múltiplas resoluções simultâneas para o mesmo serviço se já estiver em progresso
                        // Uma abordagem mais robusta gerenciaria uma fila de serviços para resolver.
                        Log.w(TAG, "Resolve listener already active. Skipping resolve for ${service.serviceName} for now.")
                        return
                    }
                    initializeResolveListener()
                    nsdManager.resolveService(service, resolveListener)
                } else if (service.serviceName == hostServiceName) {
                    Log.d(TAG, "Found own service: ${service.serviceName}. Ignoring.")
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "Service lost: ${service.serviceName}")
                // TODO: Handle service lost (e.g., remove from a list of available hosts)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
                handler.post { onDiscoveryStopped?.invoke() }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to start. Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
                handler.post { onConnectionFailed?.invoke("Falha ao iniciar descoberta NSD: $errorCode") }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to stop. Error code: $errorCode")
                // Non-critical, but good to log
            }
        }
    }
    private fun initializeResolveListener() {
        resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}. Error code: $errorCode")
                this@NetworkManager.resolveListener = null // Permitir nova tentativa de resolução
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Service resolved: ${serviceInfo.serviceName} host: ${serviceInfo.host}, port: ${serviceInfo.port}")
                // Agora podemos nos conectar a este serviço
                // Para uma UI, você adicionaria isso a uma lista de hosts disponíveis
                // e permitiria que o usuário selecionasse um para chamar connectToHost(serviceInfo).
                // Por enquanto, vamos chamar um callback genérico.
                // onHostFound(serviceInfo) -> a GameActivity decidiria conectar ou mostrar na UI

                // Exemplo:
                // if (algumaCondicaoParaConectarAutomaticamente) {
                // connectToHost(serviceInfo)
                // } else {
                // gameActivity.addAvailableHost(serviceInfo)
                // }
                // Vamos supor que a GameActivity vai gerenciar uma lista e decidir quando conectar.
                // Para este exemplo, não vamos conectar automaticamente.
                // A GameActivity precisará de uma forma de obter esses serviceInfo.
                 // Por ora, vamos invocar o onPlayerDiscovered, que é um placeholder
                val discoveredPlayerHost = Player( // Esta é uma representação MUITO simplificada
                    position = Vector3(), rotation = 0f, hp = 100, mana = 100,
                    id = serviceInfo.serviceName // Usar nome do serviço como ID temporário
                )
                // O ideal é ter um callback onHostFound(NsdServiceInfo)
                handler.post { onPlayerDiscovered?.invoke(discoveredPlayerHost) } // Isto é um placeholder!

                this@NetworkManager.resolveListener = null // Permitir nova tentativa de resolução para outros serviços
            }
        }
    }


    fun stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery: ${e.message}", e)
            } finally {
                discoveryListener = null
            }
        }
         if (resolveListener != null) { // Também limpar resolve listener se estiver ativo
            resolveListener = null // Apenas definir como nulo, pois não há "stopResolve"
        }
    }

    // --- Comunicação de Dados (Placeholders) ---
    fun sendGameState(state: GameStateUpdate) {
        // TODO: Serializar 'state' (e.g., para JSON com Kotlinx Serialization)
        // TODO: Enviar via TCP para o host (se cliente) ou para todos os clientes (se host) via seus Sockets
        // TODO: Ou enviar via UDP para atualizações frequentes
    }

    fun sendSpellCast(action: SpellCastAction) {
        // TODO: Serializar 'action'
        // TODO: Enviar via TCP
    }

    // --- Limpeza ---
    fun cleanup() {
        Log.d(TAG, "NetworkManager cleanup initiated.")
        stopDiscovery()
        cleanupHost()
        cleanupClient()
    }

    private fun cleanupHost() {
        if (registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Error unregistering service (already unregistered or invalid): ${e.message}")
            } catch (e: Exception) {
                 Log.e(TAG, "Exception during unregisterService: ${e.message}", e)
            }
            registrationListener = null
        }
        try {
            serverSocket?.close()
            Log.d(TAG, "ServerSocket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket: ${e.message}", e)
        }
        serverSocket = null
        currentPort = -1
        hostServiceName = null
        connectedClients.forEach { try { it.close() } catch (e: IOException) {} }
        connectedClients.clear()
        Log.d(TAG,"Host cleanup complete.")
    }

    private fun cleanupClient() {
        try {
            clientSocket?.close()
            Log.d(TAG, "ClientSocket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing client socket: ${e.message}", e)
        }
        clientSocket = null
        Log.d(TAG,"Client cleanup complete.")
    }
}
