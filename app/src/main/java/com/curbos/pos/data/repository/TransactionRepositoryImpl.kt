package com.curbos.pos.data.repository

import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.data.TransactionSyncManager
import com.curbos.pos.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context,
    @com.curbos.pos.di.ApplicationScope private val externalScope: kotlinx.coroutines.CoroutineScope
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

    override suspend fun createTransaction(transaction: Transaction): Result<Boolean> {
        return try {
            transactionSyncManager.stageTransaction(transaction)
            triggerSync()
            
            // Fire-and-forget processQueue for immediate retry (non-blocking) on Application Scope
            externalScope.launch {
                transactionSyncManager.processQueue()
            }
            
            // We return Success(true) optimistically because it's safely staged in local DB.
            // If the user's connection is broken, it will sync eventually in background.
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e, "Failed to stage transaction: ${e.message}")
        }
    }
    
    override suspend fun updateTransactionStatus(id: String, status: String): Result<Unit> {
        // FAST PATH: Try direct PATCH first (optimistic)
        try {
            val result = SupabaseManager.updateTransactionStatus(id, status)
            if (result is Result.Success) {
                 return Result.Success(Unit)
            }
        } catch (e: Exception) {
            // Ignore network errors here and fall back to queue
            com.curbos.pos.common.Logger.w("TransactionRepository", "Fast path failed, falling back to offline queue: ${e.message}")
        }

        // FALLBACK: Fetch > Update > Stage > Sync (Offline Capable)
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
             // If we can't even fetch it (e.g. offline and not in cache?), we can try to "guess" it if we passed the object,
             // but strictly speaking we shouldn't bump what we don't have.
             // Ideally we should check local DB (PosDao) if SupabaseManager fails (which defaults to remote).
             return Result.Error(Exception("Transaction not found for fallback update"), "Failed to update status") 
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
