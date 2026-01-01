package com.curbos.pos.data.repository

import com.curbos.pos.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import com.curbos.pos.common.Result
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward

interface TransactionRepository {
    fun getActiveTransactions(): Flow<List<Transaction>>
    suspend fun fetchActiveTransactions(): Result<List<Transaction>>
    suspend fun createTransaction(transaction: Transaction): Result<Boolean>
    suspend fun updateTransactionStatus(id: String, status: String): Result<Unit>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun syncNow()
    suspend fun subscribeToTransactionChanges(onUpdate: () -> Unit)
    suspend fun subscribeToReadyNotifications(onReady: (Transaction) -> Unit)
    
    // Loyalty
    suspend fun getCustomerByPhone(phone: String): Result<Customer?>
    suspend fun createOrUpdateCustomer(customer: Customer): Result<Customer>
    fun getLoyaltyRewards(): Flow<List<LoyaltyReward>>
    suspend fun syncRewards() // Fetch from Supabase
}
