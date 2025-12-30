package com.curbos.pos.data.repository

import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.data.TransactionSyncManager
import com.curbos.pos.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.BackoffPolicy
import com.curbos.pos.data.worker.SyncWorker
import java.util.concurrent.TimeUnit
import dagger.hilt.android.qualifiers.ApplicationContext

class TransactionRepositoryImpl @Inject constructor(
    private val transactionSyncManager: TransactionSyncManager,
    @ApplicationContext private val context: Context
) : TransactionRepository {

    private fun triggerSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag("ImmediateSync") // Tag for easier debugging
            .build()
        WorkManager.getInstance(context).enqueue(syncRequest)
        
        // Also fire a direct sync in the background to avoid waiting for WorkManager startup delay (if network is already up)
        // We'll use a detached scope or just let the caller handle it.
    }

    override fun getActiveTransactions(): Flow<List<Transaction>> = flow {
        // Initial fetch
        val result = SupabaseManager.fetchActiveTransactions()
        if (result is Result.Success) {
            emit(result.data)
        } else {
            emit(emptyList()) 
        }
    }

    override suspend fun fetchActiveTransactions(): Result<List<Transaction>> {
        return SupabaseManager.fetchActiveTransactions()
    }

    override suspend fun createTransaction(transaction: Transaction): Result<Unit> {
        return try {
            transactionSyncManager.stageTransaction(transaction)
            triggerSync()
            // Immediately attempt to process the queue in the current coroutine
            transactionSyncManager.processQueue()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to stage transaction: ${e.message}")
        }
    }
    
    override suspend fun updateTransactionStatus(id: String, status: String): Result<Unit> {
         val fetchResult = SupabaseManager.fetchTransaction(id)
         if (fetchResult is Result.Success<*>) {
             val updated = (fetchResult.data as Transaction).copy(fulfillmentStatus = status) 
             return try {
                transactionSyncManager.stageTransactionUpdate(updated)
                triggerSync()
                transactionSyncManager.processQueue()
                Result.Success(Unit)
             } catch (e: Exception) {
                Result.Error(e, "Failed to stage update: ${e.message}")
             }
         } else {
             return Result.Error(Exception("Transaction not found locally or remotely"), "Refactor pending: Use updateTransaction(Transaction) overload instead.") 
         }
    }

    override suspend fun updateTransaction(transaction: Transaction): Result<Unit> {
         return try {
            transactionSyncManager.stageTransactionUpdate(transaction)
            triggerSync()
            transactionSyncManager.processQueue()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Failed to stage update: ${e.message}")
        }
    }

    override suspend fun syncNow() {
        transactionSyncManager.processQueue()
    }

    override suspend fun subscribeToTransactionChanges(onUpdate: () -> Unit) {
        SupabaseManager.subscribeToTransactionChanges(onUpdate)
    }

    override suspend fun subscribeToReadyNotifications(onReady: (Transaction) -> Unit) {
        SupabaseManager.subscribeToReadyNotifications(onReady)
    }
}
