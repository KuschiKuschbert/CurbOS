package com.curbos.pos.data.p2p

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class P2PConnectivityManager(private val context: Context) {
    private val TAG = "P2PConnectivityManager"
    private val SERVICE_ID = "com.curbos.pos"
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO)

    // Connection State
    private val _connectionStatus = MutableStateFlow("Idle")
    val connectionStatus: StateFlow<String> = _connectionStatus
    
    private val _connectedEndpoints = MutableStateFlow<Set<String>>(emptySet())
    val connectedEndpoints: StateFlow<Set<String>> = _connectedEndpoints

    // Host Mode: Are we advertising?
    var isHosting = false
        private set

    // Received Messages
    private val _receivedMessages = MutableStateFlow<P2PMessage?>(null)
    val receivedMessages: StateFlow<P2PMessage?> = _receivedMessages

    // Callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            com.curbos.pos.common.Logger.d(TAG, "onConnectionInitiated: $endpointId, ${info.endpointName}")
            _connectionStatus.value = "Connecting to ${info.endpointName}..."
            // Auto accept for simplicity in this trusted environment
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    com.curbos.pos.common.Logger.d(TAG, "Connected to $endpointId")
                    _connectionStatus.value = "Connected"
                    val current = _connectedEndpoints.value.toMutableSet()
                    current.add(endpointId)
                    _connectedEndpoints.value = current
                    
                    if (!isHosting) {
                         Nearby.getConnectionsClient(context).stopDiscovery()
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    com.curbos.pos.common.Logger.d(TAG, "Connection rejected: $endpointId")
                    _connectionStatus.value = "Connection Rejected"
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    com.curbos.pos.common.Logger.d(TAG, "Connection error: $endpointId")
                    _connectionStatus.value = "Connection Error"
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            com.curbos.pos.common.Logger.d(TAG, "Disconnected: $endpointId")
             _connectionStatus.value = "Disconnected"
            val current = _connectedEndpoints.value.toMutableSet()
            current.remove(endpointId)
            _connectedEndpoints.value = current
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                val stringData = String(bytes, StandardCharsets.UTF_8)
                try {
                    val message = json.decodeFromString<P2PMessage>(stringData)
                    _receivedMessages.value = message
                } catch (e: Exception) {
                    com.curbos.pos.common.Logger.e(TAG, "Error parsing message: ${e.message}", e)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Optional: Track progress
        }
    }

    // Host Functions
    fun startAdvertising(nickName: String = "CurbOS-Host") {
        isHosting = true
        _connectionStatus.value = "Advertising..."
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
            
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                nickName,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                com.curbos.pos.common.Logger.d(TAG, "Advertising successfully started")
            }
            .addOnFailureListener {
                com.curbos.pos.common.Logger.e(TAG, "Advertising failed: ${it.message}", it)
                _connectionStatus.value = "Advertising Failed"
                isHosting = false
            }
    }

    fun stopAdvertising() {
        Nearby.getConnectionsClient(context).stopAdvertising()
        isHosting = false
        _connectionStatus.value = "Idle"
    }

    // Client Functions
    fun startDiscovery() {
        isHosting = false
        _connectionStatus.value = "Searching..."
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
            
        Nearby.getConnectionsClient(context)
            .startDiscovery(
                SERVICE_ID,
                object : EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                        com.curbos.pos.common.Logger.d(TAG, "Found endpoint: $endpointId, ${info.endpointName}")
                        _connectionStatus.value = "Found ${info.endpointName}"
                        // Auto connect
                        Nearby.getConnectionsClient(context)
                            .requestConnection("CurbOSDisplay", endpointId, connectionLifecycleCallback)
                    }

                    override fun onEndpointLost(endpointId: String) {
                        com.curbos.pos.common.Logger.d(TAG, "Endpoint lost: $endpointId")
                    }
                },
                discoveryOptions
            )
            .addOnSuccessListener {
                com.curbos.pos.common.Logger.d(TAG, "Discovery started")
            }
            .addOnFailureListener {
                if (it is com.google.android.gms.common.api.ApiException && it.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                     com.curbos.pos.common.Logger.d(TAG, "Already discovering.")
                     _connectionStatus.value = "Searching..."
                } else {
                    com.curbos.pos.common.Logger.e(TAG, "Discovery failed: ${it.message}", it)
                    _connectionStatus.value = "Discovery Failed"
                }
            }
    }
    
    fun stopDiscovery() {
        Nearby.getConnectionsClient(context).stopDiscovery()
        _connectionStatus.value = "Idle"
    }

    // Data Transfer
    fun sendMessage(message: P2PMessage) {
        scope.launch {
            val jsonStr = json.encodeToString(message)
            val payload = Payload.fromBytes(jsonStr.toByteArray(StandardCharsets.UTF_8))
            val endpoints = _connectedEndpoints.value.toList()
            if (endpoints.isNotEmpty()) {
                Nearby.getConnectionsClient(context).sendPayload(endpoints, payload)
            }
        }
    }
    
    fun disconnectAll() {
        Nearby.getConnectionsClient(context).stopAllEndpoints()
        _connectedEndpoints.value = emptySet()
        _connectionStatus.value = "Idle"
        isHosting = false
    }
}
