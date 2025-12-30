package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.CartItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.local.PosDao

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import com.curbos.pos.data.worker.SyncWorker
import androidx.compose.ui.platform.LocalContext
import android.app.Application

data class SalesUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val isPaymentDialogVisible: Boolean = false,
    val customerName: String = "",
    val lastTransactionId: String? = null,
    val unsyncedCount: Int = 0
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SalesViewModel @javax.inject.Inject constructor(
    private val posDao: PosDao,
    private val transactionRepository: com.curbos.pos.data.repository.TransactionRepository,
    private val p2pConnectivityManager: com.curbos.pos.data.p2p.P2PConnectivityManager
) : com.curbos.pos.common.BaseViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()
    
    init {
        // Observe Unsynced Count
        viewModelScope.launch {
            posDao.getUnsyncedCount().collect { count ->
                _uiState.update { it.copy(unsyncedCount = count) }
            }
        }
    }

    // Helper to broadcast cart
    private fun broadcastCartUpdate(cartItems: List<CartItem>) {
        if (p2pConnectivityManager.isHosting) {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true } 
            
            val displayItems = cartItems.map { 
                com.curbos.pos.data.model.TransactionItem(
                    name = it.menuItem.name,
                    price = it.totalPrice,
                    quantity = 1,
                    modifiers = it.modifiers.map { mod -> mod.name }
                )
            }
             
            try {
               // Correct usage with explicit serializer for List
               val payloadStr = json.encodeToString(
                   kotlinx.serialization.builtins.ListSerializer(com.curbos.pos.data.model.TransactionItem.serializer()), 
                   displayItems
               )
               val message = com.curbos.pos.data.p2p.P2PMessage(
                   type = com.curbos.pos.data.p2p.MESSAGE_TYPE.CART_UPDATE,
                   payload = payloadStr
               )
               p2pConnectivityManager.sendMessage(message)
            } catch (e: Exception) {
                com.curbos.pos.common.Logger.e("SalesViewModel", "Failed to broadcast cart: ${e.message}", e)
            }
        }
    }

    fun triggerManualSync(context: android.content.Context) {
        com.curbos.pos.common.Logger.d("SalesViewModel", "Manual sync triggered by user")
        
        // Use WorkManager for reliable execution
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
            
        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    val menuItems = posDao.getAllMenuItems()
    val modifiers = posDao.getAllModifiers()

    fun addToCart(item: MenuItem, selectedModifiers: List<com.curbos.pos.data.model.ModifierOption> = emptyList()) {
        _uiState.update { currentState ->
            val cartItem = CartItem(
                menuItem = item,
                modifiers = selectedModifiers
            )
            val updatedCart = currentState.cartItems + cartItem
            
            broadcastCartUpdate(updatedCart) // Broadcast!
            
            currentState.copy(
                cartItems = updatedCart,
                totalAmount = updatedCart.sumOf { it.totalPrice }
            )
        }
    }

    fun removeFromCart(item: CartItem) {
        _uiState.update { currentState ->
            val updatedCart = currentState.cartItems.toMutableList()
            updatedCart.remove(item)
            
            broadcastCartUpdate(updatedCart) // Broadcast!
            
            currentState.copy(
                cartItems = updatedCart,
                totalAmount = updatedCart.sumOf { it.totalPrice }
            )
        }
    }

    fun clearCart() {
        _uiState.update { 
            broadcastCartUpdate(emptyList()) // Broadcast!
            it.copy(cartItems = emptyList(), totalAmount = 0.0) 
        }
    }

    fun showPaymentDialog() {
        _uiState.update { it.copy(isPaymentDialogVisible = true) }
    }

    fun hidePaymentDialog() {
        _uiState.update { it.copy(isPaymentDialogVisible = false) }
    }

    fun processPayment(method: String) {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) return

        launchCatching {
            val total = currentState.totalAmount
            
            // Structured items for KDS/Stats
            val transactionItems = currentState.cartItems.map { cartItem ->
                com.curbos.pos.data.model.TransactionItem(
                    name = cartItem.menuItem.name,
                    price = cartItem.totalPrice,
                    quantity = 1,
                    modifiers = cartItem.modifiers.map { it.name }
                )
            }

            // Generate Order Number
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            val currentMax = posDao.getTodayMaxOrderNumber(startOfDay) ?: 0
            val nextOrderNumber = currentMax + 1

            val transaction = Transaction(
                id = java.util.UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                totalAmount = total,
                taxAmount = total * 0.1,
                items = transactionItems,
                status = "COMPLETED",
                paymentMethod = method,
                fulfillmentStatus = "PENDING",
                orderNumber = nextOrderNumber,
                customerName = if (currentState.customerName.isNotBlank()) currentState.customerName else null
            )

            posDao.insertTransaction(transaction)

            // 1. Send to Repository (Stages + Broadcasts + Syncs)
            com.curbos.pos.common.Logger.d("SalesViewModel", "Creating transaction ${transaction.id}...")
            transactionRepository.createTransaction(transaction)
            
            // 2. Optimistic UI Update
            com.curbos.pos.common.SnackbarManager.showSuccess("Order Up! 🌮 (${transaction.paymentMethod})")
            
            _uiState.update { 
                it.copy(
                    cartItems = emptyList(), 
                    totalAmount = 0.0, 
                    isPaymentDialogVisible = false,
                    customerName = "",
                    lastTransactionId = transaction.id
                ) 
            }
            
            // 3. Sync Trigger is now inside Repository
        }
    }


    
    fun reportError(message: String) {
        launchCatching {
             com.curbos.pos.common.SnackbarManager.showError(message)
        }
    }

    fun updateCustomerName(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    fun resetTransactionState() {
        _uiState.update { it.copy(lastTransactionId = null, customerName = "") }
    }
    
    fun reopenTransaction(transactionId: String) {
        _uiState.update { it.copy(lastTransactionId = transactionId) }
    }
}
