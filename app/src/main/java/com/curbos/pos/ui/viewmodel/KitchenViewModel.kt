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
        // Observe Local DB (Source of Truth)
        viewModelScope.launch {
            transactionRepository.getActiveTransactions().collect { orders ->
                com.curbos.pos.common.Logger.d("KitchenViewModel", "Flow emission: ${orders.size} active orders")
                _uiState.update { it.copy(activeOrders = orders, isLoading = false) }
            }
        }
        
        refresh() // Trigger initial cloud sync
        startRealtimeListening()
        startP2PListening()
        checkSettings()
    }
    
    private fun checkSettings() {
        _uiState.update { it.copy(isSimplifiedFlow = profileManager.isSimplifiedKitchenFlow()) }
    }

    // "load" is now just "sync"
    private fun loadActiveOrders() { refresh() }
    
    fun refresh() {
        com.curbos.pos.common.Logger.d("KitchenViewModel", "Triggering sync...")
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // This pulls from cloud and upserts to Local DB.
            // The DB change triggers the Flow above.
            val result = transactionRepository.fetchActiveTransactions()
            if (result is Result.Error) {
                 // Even if sync fails, we still have local data (from Flow), so just show error toast
                 _uiState.update { it.copy(error = result.message, isLoading = false) }
            } else {
                 _uiState.update { it.copy(isLoading = false, error = null) }
            }
        }
    }

    private fun startRealtimeListening() {
        viewModelScope.launch {
            transactionRepository.subscribeToTransactionChanges {
                // Repo handles the sync-on-change logic now.
                // We don't need to do anything here unless we want to log it.
                com.curbos.pos.common.Logger.d("KitchenViewModel", "Realtime update received via Repo")
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
                              // P2P Snapshot: In Local-Mode, we might want to save these?
                              // Or update memory?
                              // If we are Client, TransactionRepo usually isn't DB backed in same way?
                              // For now, let's update state directly as P2P logic is bespoke.
                              // CAUTION: This overrides Flow if mixed. 
                             _uiState.update { it.copy(activeOrders = transactions, isLoading = false) }
                        }
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_ADDED,
                        com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_UPDATED -> {
                             // Same for P2P updates.
                             val transaction = json.decodeFromString<Transaction>(message.payload)
                              _uiState.update { state ->
                                 val current = state.activeOrders.toMutableList()
                                 if (transaction.fulfillmentStatus == "COMPLETED") {
                                     current.removeAll { it.id == transaction.id }
                                 } else {
                                     val index = current.indexOfFirst { it.id == transaction.id }
                                     if (index != -1) current[index] = transaction else current.add(transaction)
                                 }
                                 state.copy(activeOrders = current)
                             }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    com.curbos.pos.common.Logger.e("KitchenViewModel", "Error handling P2P message: ${e.message}", e)
                }
            }
        }
    }

    fun bumpOrder(transaction: Transaction) {
        viewModelScope.launch {
            val isSimplified = profileManager.isSimplifiedKitchenFlow()
            
            val nextStatus = when (transaction.fulfillmentStatus) {
                "PENDING" -> if (isSimplified) "READY" else "IN_PROGRESS"
                "IN_PROGRESS" -> "READY"
                "READY" -> "COMPLETED"
                else -> "COMPLETED"
            }
            
            // P2P Client Mode
             if (!p2pConnectivityManager.isHosting && p2pConnectivityManager.connectedEndpoints.value.isNotEmpty()) {
                val updatedTx = transaction.copy(
                    fulfillmentStatus = nextStatus,
                    items = if (nextStatus == "COMPLETED") transaction.items.map { it.copy(isCompleted = true) } else transaction.items
                )
                val msg = com.curbos.pos.data.p2p.P2PMessage(
                    type = com.curbos.pos.data.p2p.MESSAGE_TYPE.STATUS_UPDATE,
                    payload = json.encodeToString(updatedTx)
                )
                p2pConnectivityManager.sendMessage(msg)
                // Optimistic UI Update for P2P Client
                 _uiState.update { state ->
                     val current = state.activeOrders.toMutableList()
                     val index = current.indexOfFirst { it.id == transaction.id }
                     if (index != -1) current[index] = updatedTx
                     state.copy(activeOrders = current)
                 }
                 return@launch
            }

            // Host/Single Mode: Update Repo (which Updates DB -> Flow -> UI)
            val updatedTx = transaction.copy(
                fulfillmentStatus = nextStatus,
                items = if (nextStatus == "COMPLETED") transaction.items.map { it.copy(isCompleted = true) } else transaction.items
            )
            
            transactionRepository.updateTransaction(updatedTx)
            // No manual state update needed!
        }
    }

    fun toggleItemCompletion(transaction: Transaction, itemIndex: Int) {
        viewModelScope.launch {
            if (itemIndex < 0 || itemIndex >= transaction.items.size) return@launch

            val currentItems = transaction.items.toMutableList()
            val item = currentItems[itemIndex]
            val newItem = item.copy(isCompleted = !item.isCompleted)
            currentItems[itemIndex] = newItem

            val allCompleted = currentItems.all { it.isCompleted }
            val nextStatus = if (allCompleted && transaction.fulfillmentStatus != "COMPLETED") {
                "READY" 
            } else {
                transaction.fulfillmentStatus
            }

            val updatedTx = transaction.copy(
                items = currentItems,
                fulfillmentStatus = nextStatus
            )

             if (!p2pConnectivityManager.isHosting && p2pConnectivityManager.connectedEndpoints.value.isNotEmpty()) {
                 val msg = com.curbos.pos.data.p2p.P2PMessage(
                    type = com.curbos.pos.data.p2p.MESSAGE_TYPE.STATUS_UPDATE,
                    payload = json.encodeToString(updatedTx)
                )
                p2pConnectivityManager.sendMessage(msg)
             } else {
                 transactionRepository.updateTransaction(updatedTx)
             }
             // No manual state update needed!
        }
    }
    
    fun completeOrder(transaction: Transaction) {
        viewModelScope.launch {
            // We should use updateTransaction(tx) instead of ID status to be safe with full objects, 
            // but status update is fine too if Repo handles it via ID.
            val result = transactionRepository.updateTransactionStatus(transaction.id, "READY")
            if (result is Result.Error) {
                _uiState.update { it.copy(error = result.message) }
            }
        }
    }
}
