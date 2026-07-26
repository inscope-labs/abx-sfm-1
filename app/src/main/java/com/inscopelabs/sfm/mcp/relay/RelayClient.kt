package com.inscopelabs.sfm.mcp.relay

import android.content.Context
import android.util.Base64
import com.inscopelabs.sfm.mcp.server.RelayConfig
import com.inscopelabs.sfm.security.encryption.EncryptionManager
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages secure relay connection with TLS and certificate pinning.
 */
class RelayClient(
    private val context: Context,
    private val config: RelayConfig,
    private val encryptionManager: EncryptionManager? = null
) {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client: OkHttpClient by lazy { buildClient() }

    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var reconnectAttempts = 0

    private var listener: RelayListener? = null

    /**
     * Connects to the relay server.
     */
    suspend fun connect(): ConnectionResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(buildUrl())
                    .apply {
                        config.authToken?.let { token ->
                            addHeader("Authorization", "Bearer $token")
                        }
                    }
                    .build()

                val ws = client.newWebSocket(request, createWebSocketListener())
                webSocket = ws

                ConnectionResult.Connecting
            } catch (e: Exception) {
                ConnectionResult.Error(e.message ?: "Connection failed")
            }
        }
    }

    /**
     * Disconnects from relay.
     */
    fun disconnect() {
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        isConnected = false
    }

    /**
     * Sends a message through relay.
     */
    suspend fun send(message: RelayMessage): SendResult {
        return withContext(Dispatchers.IO) {
            if (!isConnected) {
                return@withContext SendResult.NotConnected
            }

            try {
                val json = serializeMessage(message)
                val result = webSocket?.send(json)
                if (result == true) {
                    SendResult.Sent
                } else {
                    SendResult.Failed("Send returned false")
                }
            } catch (e: Exception) {
                SendResult.Failed(e.message ?: "Send failed")
            }
        }
    }

    /**
     * Checks if connected.
     */
    fun isConnected(): Boolean = isConnected

    /**
     * Sets relay event listener.
     */
    fun setListener(listener: RelayListener) {
        this.listener = listener
    }

    private fun buildClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (config.useTls) {
            configureTls(builder)
        }

        return builder.build()
    }

    private fun configureTls(builder: OkHttpClient.Builder) {
        if (config.certificatePinning) {
            if (config.pinnedCertificates.isEmpty()) {
                throw IllegalStateException("Certificate pinning enabled but no pinned certificates configured")
            }
            val pinnerBuilder = CertificatePinner.Builder()
            val host = extractHost(config.relayUrl)
            for (pin in config.pinnedCertificates) {
                pinnerBuilder.add(host, pin)
            }
            builder.certificatePinner(pinnerBuilder.build())
        }
    }

    private fun extractHost(relayUrl: String): String {
        return relayUrl.substringBefore("/").substringBefore(":")
    }

    private fun buildUrl(): String {
        val protocol = if (config.useTls) "wss" else "ws"
        return "$protocol://${config.relayUrl}"
    }

    fun serializeMessage(message: RelayMessage): String {
        val payloadValue = if (message.encrypted && encryptionManager != null) {
            val encryptedData = encryptionManager.encryptForCache(message.payload.toByteArray(Charsets.UTF_8))
            encryptedData.toBase64()
        } else {
            Base64.encodeToString(message.payload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

        val json = JSONObject().apply {
            put("type", message.type)
            put("payload", payloadValue)
            put("sessionId", message.sessionId)
            put("encrypted", message.encrypted)
        }
        return json.toString()
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                reconnectAttempts = 0
                startHeartbeat()
                listener?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch {
                    listener?.onMessage(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                heartbeatJob?.cancel()
                listener?.onDisconnected(reason)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                heartbeatJob?.cancel()
                listener?.onError(t.message ?: "Connection failed")
                scheduleReconnect()
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(config.heartbeatIntervalMs)
                webSocket?.send("{\"type\":\"ping\"}")
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= config.maxReconnectAttempts) {
            listener?.onMaxReconnectAttemptsReached()
            return
        }

        reconnectJob = scope.launch {
            delay(config.reconnectIntervalMs * (reconnectAttempts + 1))
            reconnectAttempts++
            connect()
        }
    }

    sealed class ConnectionResult {
        object Connecting : ConnectionResult()
        object Connected : ConnectionResult()
        data class Error(val message: String) : ConnectionResult()
    }

    sealed class SendResult {
        object Sent : SendResult()
        object NotConnected : SendResult()
        data class Failed(val reason: String) : SendResult()
    }

    interface RelayListener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onMessage(message: String)
        fun onError(error: String)
        fun onMaxReconnectAttemptsReached()
    }
}

/**
 * Represents a relay message.
 */
data class RelayMessage(
    val type: String,
    val payload: String,
    val sessionId: String,
    val encrypted: Boolean = false
)
