package com.curbos.pos.data.repository

import com.curbos.pos.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import com.curbos.pos.common.Result

interface TransactionRepository {
    fun getActiveTransactions(): Flow<List<Transaction>>
    suspend fun fetchActiveTransactions(): Result<List<Transaction>>
    suspend fun createTransaction(transaction: Transaction): Result<Boolean>
    suspend fun updateTransactionStatus(id: String, status: String): Result<Unit>
    suspend fun updateTransaction(transaction: Transaction): Result<Unit>
    suspend fun syncNow()
    suspend fun subscribeToTransactionChanges(onUpdate: () -> Unit)
    suspend fun subscribeToReadyNotifications(onReady: (Transaction) -> Unit)
}
