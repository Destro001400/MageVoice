package com.game.voicespells.core.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LanUdpSyncService(private val context: Context, private val port: Int = 55555) {
    private var socket: DatagramSocket? = null
    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    var onMessageReceived: ((String) -> Unit)? = null
    private var lastSendTime: Long = 0
    private val minSendIntervalMs = 30L // 33fps máx

    fun start() {
        running = true
        thread {
            try {
                socket = DatagramSocket(port)
                val buffer = ByteArray(4096) // buffer maior para payloads
                while (running) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charset.forName("UTF-8"))
                    handler.post { onMessageReceived?.invoke(msg) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun send(message: String, compress: Boolean = false) {
        val now = System.currentTimeMillis()
        if (now - lastSendTime < minSendIntervalMs) return // Limita frequência
        lastSendTime = now
        thread {
            try {
                var data = message.toByteArray(Charset.forName("UTF-8"))
                if (compress) {
                    data = java.util.zip.Deflater().run {
                        setInput(data)
                        finish()
                        val out = ByteArray(4096)
                        val len = deflate(out)
                        out.copyOf(len)
                    }
                }
                val packet = DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), port)
                socket?.send(packet)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        socket = null
    }
}
