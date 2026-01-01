package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.CartItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
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
    val unsyncedCount: Int = 0,
    val webBaseUrl: String = "https://prepflow.org",

    val recentTransactions: List<Transaction> = emptyList(),
    val discountAmount: Double = 0.0,
    val appliedPromoCode: String? = null,
    
    // Loyalty State
    val selectedCustomer: Customer? = null,
    val loyaltyRewards: List<LoyaltyReward> = emptyList(),
    val isLoyaltyDialogVisible: Boolean = false,
    val isScannerVisible: Boolean = false,
    val milesRedeemed: Double = 0.0,
    val allCustomers: List<Customer> = emptyList()
)

@dagger.hilt.android.lifecycle.HiltViewModel
class SalesViewModel @javax.inject.Inject constructor(
    private val posDao: PosDao,
    private val transactionRepository: com.curbos.pos.data.repository.TransactionRepository,
    private val p2pConnectivityManager: com.curbos.pos.data.p2p.P2PConnectivityManager,
    private val profileManager: com.curbos.pos.data.prefs.ProfileManager
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
        
        // Initial load of Web URL
        _uiState.update { it.copy(webBaseUrl = profileManager.getWebBaseUrl()) }

        // Observe Recent Transactions
        viewModelScope.launch {
            posDao.getAllTransactions().collect { transactions ->
                _uiState.update { it.copy(recentTransactions = transactions.take(20)) }
            }
        }

        // Observe All Customers
        viewModelScope.launch {
            transactionRepository.getAllCustomers().collect { customers ->
                _uiState.update { it.copy(allCustomers = customers) }
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
            it.copy(
                cartItems = emptyList(), 
                totalAmount = 0.0,
                discountAmount = 0.0,
                appliedPromoCode = null
            ) 
        }
    }

    fun applyPromoCode(code: String) {
        val currentState = _uiState.value
        val subtotal = currentState.cartItems.sumOf { it.totalPrice }
        
        // TODO: Replace with Database Lookup. Hardcoded for MVP/Testing.
        var discount: Double
        var validCode: String?
        val cleanCode = code.uppercase().trim()

        if (cleanCode == "SAVE10") {
            discount = subtotal * 0.10
            validCode = "SAVE10"
        } else if (cleanCode == "WELCOME" && subtotal >= 20.0) {
            discount = 5.0
            validCode = "WELCOME"
        } else if (cleanCode == "BURGERDAY" && subtotal >= 30.0) {
            discount = subtotal * 0.15
            validCode = "BURGERDAY"
        } else if (cleanCode.isBlank()) {
             // Clearing code
             discount = 0.0
             validCode = null
        } else {
             reportError("Invalid Code or Minimum Spend not met")
             return
        }

        // Ensure discount doesn't exceed total
        if (discount > subtotal) discount = subtotal

        _uiState.update { 
            it.copy(
                discountAmount = discount,
                appliedPromoCode = validCode,
                totalAmount = subtotal - discount
            )
        }
        
        if (validCode != null) {
            viewModelScope.launch {
                com.curbos.pos.common.SnackbarManager.showSuccess("Code Applied: $validCode (-$${"%.2f".format(discount)})")
            }
        }
    }

    // Loyalty Functions
    fun searchCustomer(phone: String) {
        viewModelScope.launch {
            when (val result = transactionRepository.getCustomerByPhone(phone)) {
                is com.curbos.pos.common.Result.Success -> {
                    if (result.data != null) {
                        attachCustomer(result.data)
                    } else {
                        // User not found -> Propose to create? For MVP, auto-create a stub and attach
                         val newCustomer = Customer(
                             id = UUID.randomUUID().toString(),
                             phoneNumber = phone,
                             fullName = null,
                             email = null
                         )
                         // Don't save yet, just attach. Save on checkout.
                         attachCustomer(newCustomer)
                         com.curbos.pos.common.SnackbarManager.showSuccess("New Customer Attached")
                    }
                }
                is com.curbos.pos.common.Result.Error -> {
                    reportError("Failed to find customer")
                }
                else -> {}
            }
        }
    }

    fun attachCustomer(customer: Customer) {
        _uiState.update { it.copy(selectedCustomer = customer, customerName = "") }
        // Fetch Rewards
        viewModelScope.launch {
            transactionRepository.syncRewards()
            transactionRepository.getLoyaltyRewards().collect { rewards ->
                _uiState.update { it.copy(loyaltyRewards = rewards) }
            }
        }
    }

    fun attachCustomerById(id: String) {
        launchCatching {
            val result = transactionRepository.getCustomerById(id)
            if (result is com.curbos.pos.common.Result.Success) {
                val customer = result.data
                if (customer != null) {
                    attachCustomer(customer)
                    com.curbos.pos.common.SnackbarManager.showSuccess("Customer Attached: ${customer.fullName ?: id}")
                } else {
                    reportError("Customer not found.")
                }
            } else {
                reportError("Error fetching customer.")
            }
        }
    }

    fun detachCustomer() {
        _uiState.update { 
            it.copy(
                selectedCustomer = null, 
                milesRedeemed = 0.0,
                discountAmount = 0.0 // Reset discount if removing customer? Maybe ask user. For now, reset.
            ) 
        }
    }

    fun searchAllCustomers(query: String) {
        viewModelScope.launch {
            transactionRepository.searchCustomers(query).collect { customers ->
                _uiState.update { it.copy(allCustomers = customers) }
            }
        }
    }

    fun syncAllCustomers() {
        launchCatching {
            val result = transactionRepository.syncAllCustomers()
            if (result is com.curbos.pos.common.Result.Success) {
                com.curbos.pos.common.SnackbarManager.showSuccess("Customer database synced!")
            } else {
                reportError("Sync failed: ${(result as com.curbos.pos.common.Result.Error).message}")
            }
        }
    }

    fun redeemReward(reward: LoyaltyReward) {
        val currentState = _uiState.value
        val customer = currentState.selectedCustomer ?: return
        
        if (customer.redeemableMiles < (currentState.milesRedeemed + reward.costMiles)) {
             reportError("Not enough miles!")
             return
        }
        
        // Define Discount Amounts (Hardcoded Logic mapping to DB Reward descriptions)
        // Ideally DB has amount column, but per plan description is text.
        val discountValue = when {
            reward.description.contains("Free Drink", true) -> 3.50
            reward.description.contains("Free Taco", true) -> 5.00
            else -> 0.0 // Merch etc might be handled differently
        }
        
        if (discountValue > 0) {
            val newDiscount = currentState.discountAmount + discountValue
            val subtotal = currentState.cartItems.sumOf { it.totalPrice }
             
             _uiState.update { 
                 it.copy(
                     discountAmount = minOf(newDiscount, subtotal),
                     milesRedeemed = it.milesRedeemed + reward.costMiles,
                     isLoyaltyDialogVisible = false
                 )
             }

             viewModelScope.launch {
                 com.curbos.pos.common.SnackbarManager.showSuccess("Redeemed: ${reward.description}")
             }
        } else {
             reportError("This reward cannot be auto-applied yet.")
        }
    }
    
    fun showLoyaltyDialog() {
        _uiState.update { it.copy(isLoyaltyDialogVisible = true) }
    }
    
    fun hideLoyaltyDialog() {
        _uiState.update { it.copy(isLoyaltyDialogVisible = false) }
    }

    fun showScanner() {
        _uiState.update { it.copy(isScannerVisible = true) }
    }

    fun hideScanner() {
        _uiState.update { it.copy(isScannerVisible = false) }
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
                discountAmount = currentState.discountAmount,
                promoCode = currentState.appliedPromoCode,
                taxAmount = total * 0.1,
                items = transactionItems,
                status = "COMPLETED",
                paymentMethod = method,
                fulfillmentStatus = "PENDING",

                orderNumber = nextOrderNumber,
                customerName = if (currentState.customerName.isNotBlank()) currentState.customerName else currentState.selectedCustomer?.fullName,
                customerId = currentState.selectedCustomer?.id,
                milesEarned = total, // 1 point per $1
                milesRedeemed = currentState.milesRedeemed
            )

            posDao.insertTransaction(transaction)

            // 1. Send to Repository and wait for Sync result
            com.curbos.pos.common.Logger.d("SalesViewModel", "Creating transaction ${transaction.id}...")
            val result = transactionRepository.createTransaction(transaction)
            
            when (result) {
                is com.curbos.pos.common.Result.Success -> {
                    val message = "Order Up! 🌮 (${transaction.paymentMethod})"
                    
                    com.curbos.pos.common.SnackbarManager.showSuccess(message)
                    
                    _uiState.update { 
                        it.copy(
                            cartItems = emptyList(), 
                            totalAmount = 0.0, 
                            isPaymentDialogVisible = false,
                            customerName = "",
                            lastTransactionId = transaction.id,
                            
                            // Reset Loyalty
                            selectedCustomer = null,
                            milesRedeemed = 0.0,
                            discountAmount = 0.0
                        ) 
                    }
                    
                    // Update Customer Loyalty Points
                    currentState.selectedCustomer?.let { customer ->
                        val earned = transaction.totalAmount 
                        val redeemed = currentState.milesRedeemed
                        
                        // Calculate new values
                        val newLifetime = customer.lifetimeMiles + earned
                        // Redeemable: Old - Redeemed + Earned
                        val newRedeemable = customer.redeemableMiles - redeemed + earned
                        
                        val updatedCustomer = customer.copy(
                            lifetimeMiles = newLifetime,
                            redeemableMiles = newRedeemable
                        )
                        com.curbos.pos.common.Logger.d("SalesViewModel", "Updating Customer Points: +$earned, -$redeemed")
                        launchCatching {
                            transactionRepository.createOrUpdateCustomer(updatedCustomer)
                        }
                    }
                }
                is com.curbos.pos.common.Result.Error -> {
                    reportError("Failed to save order: ${result.message}")
                }
                else -> {}
            }
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
