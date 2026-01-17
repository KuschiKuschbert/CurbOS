package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.CartItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.LoyaltyConstants
import com.curbos.pos.data.LoyaltyRepository

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
    val loyaltyDialogTab: Int = 0,
    val isScannerVisible: Boolean = false,
    val milesRedeemed: Double = 0.0,
    val allCustomers: List<Customer> = emptyList(),
    val customerHistory: List<Transaction> = emptyList() // Added for Mini-Profile
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

    // Raw Menu Items
    private val allMenuItems = posDao.getAllMenuItems()
    val modifiers = posDao.getAllModifiers()
    
    // Filtered Items (Secret Menu Logic)
    val menuItems: kotlinx.coroutines.flow.Flow<List<MenuItem>> = kotlinx.coroutines.flow.combine(
        allMenuItems,
        _uiState
    ) { items, state ->
        items.filter { item ->
            val reqRank = item.requiredRank
            if (reqRank == null) true // Public item
            else {
                // Secret Item! Check Customer Rank.
                val customer = state.selectedCustomer
                if (customer == null) false // Hidden for guests
                else {
                    // Check Rank Hierarchy
                    val myRankCfg = LoyaltyRepository.getRankByName(customer.currentRank)
                    val reqRankCfg = LoyaltyRepository.getRankByName(reqRank)
                    
                    if (myRankCfg != null && reqRankCfg != null) {
                        myRankCfg.minMiles >= reqRankCfg.minMiles
                    } else false
                }
            }
        }
    }

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

    fun toggleLoyaltyDialog(visible: Boolean, tab: Int = 0) {
        _uiState.update { it.copy(isLoyaltyDialogVisible = visible, loyaltyDialogTab = tab) }
        if (visible) syncAllCustomers()
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
        _uiState.update { it.copy(
            selectedCustomer = customer, 
            customerName = customer.fullName ?: it.customerName 
        ) }
        // Fetch Rewards
        viewModelScope.launch {
            transactionRepository.syncRewards()
            transactionRepository.getLoyaltyRewards().collect { rewards ->
                _uiState.update { it.copy(loyaltyRewards = rewards) }
            }
        }
        
        // Fetch History (Native Passport)
        viewModelScope.launch {
            transactionRepository.getTransactionsByCustomer(customer.id).collect { history ->
                _uiState.update { it.copy(customerHistory = history) }
            }
        }
    }

    fun attachCustomerById(input: String) {
        launchCatching {
            // Handle Passport URLs (e.g. https://prepflow.org/curbos/quests/UUID)
            val id = if (input.contains("/curbos/quests/")) {
                input.substringAfterLast("/")
            } else {
                input
            }

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
                discountAmount = 0.0, // Reset discount if removing customer
                customerHistory = emptyList() // Clear history
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
                com.curbos.pos.common.SnackbarManager.showSuccess("Customer database synced! ☁️")
            } else {
                reportError("Sync failed: ${(result as com.curbos.pos.common.Result.Error).message}")
            }
        }
    }

    fun pushCustomersToCloud() {
        launchCatching {
            val result = transactionRepository.pushAllCustomers()
            if (result is com.curbos.pos.common.Result.Success) {
                com.curbos.pos.common.SnackbarManager.showSuccess("All Customers Uploaded to Cloud! ☁️")
            } else {
                reportError("Upload failed: ${(result as com.curbos.pos.common.Result.Error).message}")
            }
        }
    }

    fun redeemReward(reward: LoyaltyReward) {
        val currentState = _uiState.value
        val customer = currentState.selectedCustomer ?: return
        
        // 1. Check if customer has enough miles
        if (customer.redeemableMiles < (currentState.milesRedeemed + reward.costMiles)) {
             reportError("Not enough miles!")
             return
        }
        
        // 2. Handle Auto-Applied (Discount) Rewards
        if (reward.isAutoApplied && reward.discountAmount > 0) {
            val newDiscount = currentState.discountAmount + reward.discountAmount
            val subtotal = currentState.cartItems.sumOf { it.totalPrice }
             
             _uiState.update { 
                 it.copy(
                     discountAmount = minOf(newDiscount, subtotal),
                     totalAmount = subtotal - minOf(newDiscount, subtotal),
                     milesRedeemed = it.milesRedeemed + reward.costMiles,
                     isLoyaltyDialogVisible = false
                 )
             }

             viewModelScope.launch {
                 com.curbos.pos.common.SnackbarManager.showSuccess("Applied: ${reward.description} (-$${"%.2f".format(reward.discountAmount)})")
             }
        } else if (!reward.isAutoApplied) {
            // 3. Handle Manual Rewards (Merch, etc.)
            // We just deduct the points but don't apply a dollar discount to the cart.
             _uiState.update { 
                 it.copy(
                     milesRedeemed = it.milesRedeemed + reward.costMiles,
                     isLoyaltyDialogVisible = false
                 )
             }
             viewModelScope.launch {
                 com.curbos.pos.common.SnackbarManager.showSuccess("Redeemed: ${reward.description} (Collect from Counter)")
             }
        } else {
             reportError("This reward has no defined discount amount.")
        }
    }

    fun applyBonusMiles(action: LoyaltyConstants.BonusAction) {
        val currentState = _uiState.value
        val customer = currentState.selectedCustomer ?: return
        
        val earned = action.miles.toDouble()
        val currentLifetime = customer.lifetimeMiles
        val currentRedeemable = customer.redeemableMiles
        
        val newLifetime = currentLifetime + earned
        val newRedeemable = currentRedeemable + earned
        
        val newRank = LoyaltyRepository.getRankForMiles(newLifetime)
        
        val updatedCustomer = customer.copy(
            lifetimeMiles = newLifetime,
            redeemableMiles = newRedeemable,
            currentRank = newRank
        )
        
        launchCatching {
            transactionRepository.createOrUpdateCustomer(updatedCustomer)
            
            // Optimistic update UI
            _uiState.update { it.copy(selectedCustomer = updatedCustomer) }
            
            com.curbos.pos.common.SnackbarManager.showSuccess("Bonus Applied: ${action.title} (+${action.miles})")
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

            // posDao.insertTransaction(transaction) -- Redundant, handled by Repository now.

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
                    
                    // Update Customer Loyalty Points & Advanced Features
                    currentState.selectedCustomer?.let { customer ->
                        val earned = transaction.totalAmount 
                        val redeemed = currentState.milesRedeemed
                        
                        // 1. Core Points & Rank
                        val newLifetime = customer.lifetimeMiles + earned
                        val newRedeemable = customer.redeemableMiles - redeemed + earned
                        val newRank = LoyaltyRepository.getRankForMiles(newLifetime)
                        
                        // 2. Region Unlocks
                        val regionsInOrder = currentState.cartItems.mapNotNull { it.menuItem.region }.distinct()
                        val newUnlockedRegions = (customer.unlockedRegions + regionsInOrder).distinct()
                        
                        // 3. Hot Streaks (Weekly)
                        val now = System.currentTimeMillis()
                        val newStreak = calculateNewStreak(customer.lastVisit, now, customer.streakCount)
                        
                        // 4. Stamp Cards
                        val newStampCards = updateStampCards(customer.stampCards, currentState.cartItems)
                        
                        // 5. Quests
                        val newQuests = updateQuests(customer.activeQuests)

                        val updatedCustomer = customer.copy(
                            lifetimeMiles = newLifetime,
                            redeemableMiles = newRedeemable,
                            currentRank = newRank,
                            unlockedRegions = newUnlockedRegions,
                            lastVisit = now,
                            streakCount = newStreak,
                            stampCards = newStampCards,
                            activeQuests = newQuests
                        )
                        com.curbos.pos.common.Logger.d("SalesViewModel", "Loyalty Update: Streak=$newStreak, Stamps=$newStampCards")
                        launchCatching {
                            transactionRepository.createOrUpdateCustomer(updatedCustomer)
                            checkQuestCompletions(newQuests) // Notify user if quest completed
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

    // --- Advanced Loyalty Logic ---

    private fun calculateNewStreak(lastVisit: Long, now: Long, currentStreak: Int): Int {
        if (lastVisit == 0L) return 1
        
        val calLast = java.util.Calendar.getInstance().apply { timeInMillis = lastVisit }
        val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
        
        val lastWeek = calLast.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentWeek = calNow.get(java.util.Calendar.WEEK_OF_YEAR)
        val lastYear = calLast.get(java.util.Calendar.YEAR)
        val currentYear = calNow.get(java.util.Calendar.YEAR)
        
        // Same week? No change.
        if (lastYear == currentYear && lastWeek == currentWeek) return currentStreak
        
        // Previous week? Increment.
        // TODO: Handle year boundary properly (Week 52 -> Week 1)
        val isConsecutive = (currentYear == lastYear && currentWeek == lastWeek + 1) ||
                            (currentYear == lastYear + 1 && currentWeek == 1 && (lastWeek == 52 || lastWeek == 53))
                            
        return if (isConsecutive) currentStreak + 1 else 1
    }

    private fun updateStampCards(currentCards: Map<String, Int>, cartItems: List<CartItem>): Map<String, Int> {
        val newCards = currentCards.toMutableMap()
        
        cartItems.forEach { item ->
            // Assume Category is the key
            val category = item.menuItem.category
            // Log the punch action
            com.curbos.pos.common.Logger.d("SalesViewModel", "Punching card for category: $category")
            
            val currentCount = newCards[category] ?: 0
            newCards[category] = currentCount + 1
        }
        return newCards
    }

    private fun updateQuests(activeQuests: List<com.curbos.pos.data.model.QuestProgress>): List<com.curbos.pos.data.model.QuestProgress> {
        // Simple logic: if quest is "BUY_X" and target matches, increment.
        // For MVP, since we don't have the Quest Definition here easily (only ID), we might need to fetch Quests first or store definition in Progress.
        // Skipping complex Quest Logic for Phase 2 MVP - we will just preserve existing.
        return activeQuests 
    }
    
    private suspend fun checkQuestCompletions(quests: List<com.curbos.pos.data.model.QuestProgress>) {
        val completed = quests.filter { it.isCompleted } // If we tracked 'just completed'
        if (completed.isNotEmpty()) {
             com.curbos.pos.common.SnackbarManager.showSuccess("Quest Completed! 🎯")
        }
    }
}
