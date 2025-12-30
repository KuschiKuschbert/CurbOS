package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.model.Transaction

import com.curbos.pos.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerDisplayUiState(
    val preparingOrders: List<Transaction> = emptyList(),
    val readyOrders: List<Transaction> = emptyList(),
    val liveCart: List<com.curbos.pos.data.model.TransactionItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@dagger.hilt.android.lifecycle.HiltViewModel
class CustomerDisplayViewModel @javax.inject.Inject constructor(
    private val p2pConnectivityManager: com.curbos.pos.data.p2p.P2PConnectivityManager,
    private val transactionRepository: com.curbos.pos.data.repository.TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerDisplayUiState())
    val uiState: StateFlow<CustomerDisplayUiState> = _uiState.asStateFlow()
    
    // JSON for decoding P2P messages
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    init {
        loadActiveOrders()
        startRealtimeListening()
        startP2PListening()
        startPolling()
    }
    
    private fun startPolling() {
        viewModelScope.launch {
            while(true) {
                kotlinx.coroutines.delay(15_000) // 15 seconds
                loadActiveOrders(showLoading = false)
            }
        }
    }
    
    private fun startP2PListening() {
        
        viewModelScope.launch {
            p2pConnectivityManager.receivedMessages.collect { message ->
                if (message == null) return@collect
                
                when (message.type) {
                    com.curbos.pos.data.p2p.MESSAGE_TYPE.CART_UPDATE -> {
                        try {
                            val items = json.decodeFromString<List<com.curbos.pos.data.model.TransactionItem>>(message.payload)
                            _uiState.update { it.copy(liveCart = items) }
                        } catch (e: Exception) {
                            com.curbos.pos.common.Logger.e("CustomerDisplayVM", "Error parsing cart: ${e.message}", e)
                        }
                    }
                    com.curbos.pos.data.p2p.MESSAGE_TYPE.SNAPSHOT,
                    com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_ADDED,
                    com.curbos.pos.data.p2p.MESSAGE_TYPE.ORDER_UPDATED -> {
                         // Eventually standard KDS logic should also listen here if we want full offline KDS.
                         // For now, let's just trigger a reload or handle specific payloads if we implement full offline KDS in this VM too.
                         // But specific request is for "Customer Display" (Cart).
                         // We can re-use loadActiveOrders logic if we had a local data source synced via P2P.
                         // Since we are "offline", active orders might not update via Supabase.
                         // TODO: Implement Offline Order Status sync (optional for now, focus on Cart).
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadActiveOrders(showLoading: Boolean = true) {
        if (showLoading) _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = transactionRepository.fetchActiveTransactions()) {
                is Result.Success -> {
                    processOrders(result.data)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = result.message, isLoading = false) }
                }
                else -> {}
            }
        }
    }

    private fun processOrders(transactions: List<Transaction>) {
        val preparing = transactions.filter { 
            it.fulfillmentStatus == "PENDING" || it.fulfillmentStatus == "IN_PROGRESS" 
        }.sortedBy { it.timestamp }

        val ready = transactions.filter { 
            it.fulfillmentStatus == "READY" 
        }.sortedByDescending { it.timestamp }
        
        _uiState.update { 
            it.copy(
                preparingOrders = preparing, 
                readyOrders = ready.sortedBy { tx -> tx.timestamp },
                isLoading = false,
                error = null
            ) 
        }
    }

    private fun startRealtimeListening() {
        viewModelScope.launch {
            transactionRepository.subscribeToTransactionChanges {
                loadActiveOrders(showLoading = false)
            }
        }
    }
}
