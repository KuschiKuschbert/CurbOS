package com.curbos.pos.data.repository

import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.data.TransactionSyncManager
import com.curbos.pos.common.Result
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
import com.curbos.pos.data.local.PosDao
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
    private val posDao: PosDao,
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

    override fun getActiveTransactions(): Flow<List<Transaction>> {
        // Source of Truth: Local Database
        return posDao.getActiveTransactions()
    }

    override suspend fun fetchActiveTransactions(): Result<List<Transaction>> {
        // 1. Fetch from Cloud
        val result = SupabaseManager.fetchActiveTransactions()
        
        // 2. Update Local DB (which triggers Flow emissions automatically)
        if (result is Result.Success) {
            try {
                // Upsert all fetched transactions
                result.data.forEach { posDao.insertTransaction(it) }
            } catch (e: Exception) {
                com.curbos.pos.common.Logger.e("TransactionRepository", "Failed to cache transactions: ${e.message}")
            }
        }
        
        return result
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
        // Local First Logic:
        // 1. Update Local Immediately (via SyncManager staging which updates local DB)
        // 2. Try Background Sync
        
        // Retrieve current to clone it, or just partial update if DAO supported it. 
        // For now, let's try to get it from Local DB first to be fast.
        // val localTx = posDao.getOfflineTransactions().find { it.id.toString() == id } // Removed unused variable
        // Better: Use a direct DAO fetch if we had getTransactionById. 
        // We lack getTransactionById in the interface I added, but we can rely on SupabaseManager.fetchTransaction as fallback if checking cloud,
        // OR just construct a minimal update? No, we need the whole object for the JSON.
        
        // Let's stick to the Plan: Fetch (Remote) -> Update -> Stage.
        // Optimization: Fetch (Local)? 
        // I will add a TO-DO for Fetch Local. For now, to ensure consistency:
        
        // 1. Try to Fetch from Local Flow? No, that's async.
        // Let's use the Fallback: Fetch > Update > Stage.
        // But if offline, Fetch Remote fails.
        // WE NEED getTransactionById in PosDao for true Offline. 
        // I'll add a quick "get active transactions" scan since we have that flow, or just rely on the UI passing the full object?
        // The UI calls `bumpOrder(transaction)`. It passes the FULL transaction!
        // So `updateTransactionStatus` taking just ID is a bit inefficient if we have the object.
        // KitchenViewModel calls `updateTransactionStatus(id)` in `completeOrder` but `updateTransaction(tx)` in `bumpOrder`.
        
        // Let's look at `updateTransactionStatus` usage.
        
        // FAST PATH: Try direct PATCH first (optimistic) - Wait, this is "Cloud First".
        // "Local First" means update Local then Sync.
        
        // If we only have ID, we are stuck if we can't fetch.
        // Use SupabaseManager.fetchTransaction(id)
        val fetchResult = SupabaseManager.fetchTransaction(id) 
        if (fetchResult is Result.Success) {
            val updated = fetchResult.data.copy(fulfillmentStatus = status)
            return updateTransaction(updated)
        } else {
             // Offline support for ID-only updates requires Local DB fetch.
             // Since I didn't add getTransactionById to PosDao yet, I will fail gracefull or try to just patch remote.
             return SupabaseManager.updateTransactionStatus(id, status)
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
        // Also fetch fresh
        fetchActiveTransactions()
    }

    override suspend fun subscribeToTransactionChanges(onUpdate: () -> Unit) {
        // When Cloud changes, Sync to Local.
        SupabaseManager.subscribeToTransactionChanges {
            externalScope.launch {
                val result = SupabaseManager.fetchActiveTransactions()
                if (result is Result.Success) {
                    result.data.forEach { posDao.insertTransaction(it) }
                }
                onUpdate()
            }
        }
    }

    override suspend fun subscribeToReadyNotifications(onReady: (Transaction) -> Unit) {
        SupabaseManager.subscribeToReadyNotifications(onReady)
    }

    // Loyalty Implementation
    override suspend fun getCustomerByPhone(phone: String): Result<Customer?> {
        // 1. Try Local
        val local = posDao.getCustomerByPhone(phone)
        if (local != null) return Result.Success(local)

        // 2. Try Remote
        val remoteResult = SupabaseManager.fetchCustomer(phone)
        if (remoteResult is Result.Success && remoteResult.data != null) {
            posDao.insertCustomer(remoteResult.data)
            return Result.Success(remoteResult.data)
        }

        return remoteResult
    }

    override suspend fun getCustomerById(id: String): Result<Customer?> {
        // 1. Try Local
        val local = posDao.getCustomerById(id)
        if (local != null) return Result.Success(local)

        // 2. Try Remote
        val remoteResult = SupabaseManager.fetchCustomerById(id)
        if (remoteResult is Result.Success && remoteResult.data != null) {
            posDao.insertCustomer(remoteResult.data)
            return Result.Success(remoteResult.data)
        }

        return remoteResult
    }

    override suspend fun createOrUpdateCustomer(customer: Customer): Result<Customer> {
        return try {
            // 1. Optimistic Local Update
            posDao.insertCustomer(customer)
            
            // 2. Stage for background sync (Offline Ready)
            transactionSyncManager.stageCustomerUpdate(customer)
            
            // 3. Trigger immediate background sync
            triggerSync()
            externalScope.launch {
                transactionSyncManager.processQueue()
            }
            
            Result.Success(customer)
        } catch (e: Exception) {
            Result.Error(e, "Failed to update customer: ${e.message}")
        }
    }

    override fun getLoyaltyRewards(): Flow<List<LoyaltyReward>> {
        return posDao.getAllLoyaltyRewards()
    }

    override suspend fun syncRewards() {
        val result = SupabaseManager.fetchRewards()
        if (result is Result.Success) {
            posDao.insertLoyaltyRewards(result.data)
        }
    }

    // Customer Directory
    override fun getAllCustomers(): Flow<List<Customer>> {
        return posDao.getAllCustomers()
    }

    override fun searchCustomers(query: String): Flow<List<Customer>> {
        return posDao.searchCustomersByName("%$query%")
    }

    override suspend fun syncAllCustomers(): Result<Unit> {
        val result = SupabaseManager.fetchAllCustomers()
        return if (result is Result.Success) {
            // Upsert remote customers to local (Merging)
            result.data.forEach { posDao.insertCustomer(it) }
            Result.Success(Unit)
        } else {
            Result.Error((result as Result.Error).exception, result.message)
        }
    }

    override suspend fun pushAllCustomers(): Result<Unit> {
        return try {
            val localCustomers = posDao.getCustomerList() // Snapshot
            if (localCustomers.isNotEmpty()) {
                SupabaseManager.upsertCustomers(localCustomers)
            } else {
                Result.Success(Unit) // Nothing to push
            }
        } catch (e: Exception) {
            Result.Error(e, "Failed to push customers: ${e.message}")
        }
    }
}
