package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.repository.TransactionRepository
import com.curbos.pos.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString


data class KitchenUiState(
    val activeOrders: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSimplifiedFlow: Boolean = false
)

@dagger.hilt.android.lifecycle.HiltViewModel
class KitchenViewModel @javax.inject.Inject constructor(
    private val profileManager: com.curbos.pos.data.prefs.ProfileManager,
    private val p2pConnectivityManager: com.curbos.pos.data.p2p.P2PConnectivityManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KitchenUiState())
    val uiState: StateFlow<KitchenUiState> = _uiState.asStateFlow()

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    init {
        loadActiveOrders()
        startRealtimeListening()
        startP2PListening()
        checkSettings()
    }
    
    private fun checkSettings() {
        _uiState.update { it.copy(isSimplifiedFlow = profileManager.isSimplifiedKitchenFlow()) }
    }

    private fun loadActiveOrders() {
        com.curbos.pos.common.Logger.d("KitchenViewModel", "Fetching active orders...")
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = transactionRepository.fetchActiveTransactions()) {
                is Result.Success -> {
                    com.curbos.pos.common.Logger.d("KitchenViewModel", "Successfully fetched ${result.data.size} orders")
                    _uiState.update { it.copy(activeOrders = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    // In P2P Client mode, failure to fetch from Supabase is expected.
                    // We rely on P2P Snapshots/Updates.
                    com.curbos.pos.common.Logger.e("KitchenViewModel", "Error fetching orders: ${result.message}")
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    private fun startRealtimeListening() {
        viewModelScope.launch {
            transactionRepository.subscribeToTransactionChanges {
                loadActiveOrders()
            }
        }
    }

    private fun startP2PListening() {
        viewModelScope.launch {
            p2pConnectivityManager.receivedMessages.collect { message ->
                if (message == null) return@collect
                try {
                    when (message.type) {
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.SNAPSHOT -> {
                             val transactions = json.decodeFromString<List<Transaction>>(message.payload)
                             _uiState.update { it.copy(activeOrders = transactions, isLoading = false) }
                        }
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_ADDED,
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_UPDATED -> {
                             val transaction = json.decodeFromString<Transaction>(message.payload)
                             _uiState.update { state ->
                                 val current = state.activeOrders.toMutableList()
                                 if (transaction.fulfillmentStatus == "COMPLETED" || transaction.status == "COMPLETED") {
                                     current.removeAll { it.id == transaction.id }
                                 } else {
                                     val index = current.indexOfFirst { it.id == transaction.id }
                                     if (index != -1) {
                                         current[index] = transaction
                                     } else {
                                         current.add(transaction)
                                     }
                                 }
                                 state.copy(activeOrders = current)
                             }
                        }
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.STATUS_UPDATE -> {
                              // If we are Host, we received a status update from Client.
                              if (p2pConnectivityManager.isHosting) {
                                  val updatedTx = json.decodeFromString<Transaction>(message.payload)
                                  // Update Supabase directly with the status from the client
                                  launch {
                                      transactionRepository.updateTransactionStatus(updatedTx.id, updatedTx.fulfillmentStatus)
                                       _uiState.update { state ->
                                             val current = state.activeOrders.toMutableList()
                                             val index = current.indexOfFirst { it.id == updatedTx.id }
                                             if (index != -1) current[index] = updatedTx
                                             state.copy(activeOrders = current)
                                       }
                                  }
                              }
                        }
                        else -> { 
                            // Ignore CART_UPDATE or other future messages in Kitchen View
                        }
                    }
                } catch (e: Exception) {
                    com.curbos.pos.common.Logger.e("KitchenViewModel", "Error handling P2P message: ${e.message}", e)
                }
            }
        }
    }

    fun bumpOrder(transaction: Transaction) {
        viewModelScope.launch {
            // Check settings freshly in case they changed from another screen (though less likely in KDS view)
            // But we use the state one for UI.
            val isSimplified = profileManager.isSimplifiedKitchenFlow()
            
            val nextStatus = when (transaction.fulfillmentStatus) {
                "PENDING" -> if (isSimplified) "READY" else "IN_PROGRESS"
                "IN_PROGRESS" -> "READY"
                "READY" -> "COMPLETED"
                else -> "COMPLETED"
            }
            
            // P2P Client Mode: Send Status Update to Host
            if (!p2pConnectivityManager.isHosting && p2pConnectivityManager.connectedEndpoints.value.isNotEmpty()) {
                val updatedTx = transaction.copy(fulfillmentStatus = nextStatus)
                val msg = com.curbos.pos.data.p2p.P2PMessage(
                    type = com.curbos.pos.data.p2p.MESSAGE_TYPE.STATUS_UPDATE,
                    payload = json.encodeToString(updatedTx)
                )
                p2pConnectivityManager.sendMessage(msg)
                
                // Optimistically update local UI
                 _uiState.update { state ->
                     val current = state.activeOrders.toMutableList()
                     val index = current.indexOfFirst { it.id == transaction.id }
                     if (index != -1) current[index] = updatedTx
                     state.copy(activeOrders = current)
                 }
                 return@launch
            }

            // Online / Host Mode
            // Robust Sync: Always stage locally first, broadast, and try to sync.
            val updatedTx = transaction.copy(fulfillmentStatus = nextStatus)
            
            launch {
                // Repository handles staging + triggering SyncWorker details
                transactionRepository.updateTransaction(updatedTx)
                
                // Optimistically update UI
                 _uiState.update { state ->
                     val current = state.activeOrders.toMutableList()
                     if (updatedTx.fulfillmentStatus == "COMPLETED") {
                         current.removeAll { it.id == updatedTx.id }
                     } else {
                         val index = current.indexOfFirst { it.id == updatedTx.id }
                         if (index != -1) current[index] = updatedTx
                     }
                     state.copy(activeOrders = current)
                 }
            }
        }
    }
    
    fun refresh() {
        loadActiveOrders()
        checkSettings()
    }

    fun completeOrder(transaction: Transaction) {
        viewModelScope.launch {
            val result = transactionRepository.updateTransactionStatus(transaction.id, "READY")
            if (result is Result.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }
}
