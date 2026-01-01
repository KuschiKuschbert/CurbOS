package com.curbos.pos.data

import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.OfflineTransaction
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.remote.SupabaseManager
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import com.curbos.pos.common.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import com.curbos.pos.data.p2p.P2PConnectivityManager
import com.curbos.pos.data.p2p.P2PMessage
import com.curbos.pos.data.p2p.MESSAGE_TYPE
import com.curbos.pos.data.model.OfflineCustomerUpdate
import com.curbos.pos.data.model.Customer

@javax.inject.Singleton
class TransactionSyncManager @javax.inject.Inject constructor(
    private val posDao: PosDao,
    private val p2pConnectivityManager: P2PConnectivityManager,
    @com.curbos.pos.di.ApplicationScope private val externalScope: kotlinx.coroutines.CoroutineScope,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
) {

    private val syncMutex = Mutex()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun stageTransaction(transaction: Transaction) {
        try {
            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Staging transaction ${transaction.id}")
            // 1. Store locally for safety
            val jsonString = json.encodeToString(transaction)
            val offlineTx = OfflineTransaction(transactionJson = jsonString)
            posDao.insertOfflineTransaction(offlineTx)
            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Offline record staged.")

            // Broadcast to P2P Clients if Hosting
            if (p2pConnectivityManager.isHosting) {
                 val message = P2PMessage(
                     type = MESSAGE_TYPE.ORDER_ADDED,
                     payload = jsonString
                 )
                 p2pConnectivityManager.sendMessage(message)
                 com.curbos.pos.common.Logger.d("TransactionSyncManager", "Broadcasted order ${transaction.id} to P2P clients.")
            }
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("TransactionSyncManager", "Staging error: ${e.message}", e)
            e.printStackTrace()
            throw e // Re-throw to inform UI of failure to save
        }
    }

    suspend fun stageTransactionUpdate(transaction: Transaction) {
        try {
            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Staging update for ${transaction.id}")
            // 1. Store locally for safety (Offline Queue)
            val jsonString = json.encodeToString(transaction)
            val offlineTx = OfflineTransaction(transactionJson = jsonString)
            posDao.insertOfflineTransaction(offlineTx)
            
            // Broadcast to P2P Clients if Hosting
            if (p2pConnectivityManager.isHosting) {
                 val message = P2PMessage(
                     type = MESSAGE_TYPE.ORDER_UPDATED,
                     payload = jsonString
                 )
                 p2pConnectivityManager.sendMessage(message)
                 com.curbos.pos.common.Logger.d("TransactionSyncManager", "Broadcasted update ${transaction.id} to P2P clients.")
            }
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("TransactionSyncManager", "Staging update error: ${e.message}", e)
            throw e
        }
    }

    suspend fun stageCustomerUpdate(customer: Customer) {
        try {
            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Staging customer update for ${customer.id}")
            val jsonString = json.encodeToString(customer)
            val offlineUpdate = OfflineCustomerUpdate(customerJson = jsonString)
            posDao.insertOfflineCustomerUpdate(offlineUpdate)
            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Customer update staged.")
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("TransactionSyncManager", "Staging customer update error: ${e.message}", e)
            throw e
        }
    }

    suspend fun processQueue() {
        processQueueWithResult()
    }

    /**
     * Processes queue and returns true if all items were processed successfully (or removed on permanent error).
     * Returns false if any item failed due to network/transient error.
     */
    suspend fun processQueueWithResult(): Boolean {
        syncMutex.withLock {
            try {
                val offlineTransactions = posDao.getOfflineTransactions()
                val offlineCustomers = posDao.getOfflineCustomerUpdates()
                
                if (offlineTransactions.isEmpty() && offlineCustomers.isEmpty()) return true

                com.curbos.pos.common.Logger.d("TransactionSyncManager", "Processing queues: ${offlineTransactions.size} transactions, ${offlineCustomers.size} customers")

                var allSuccess = true

                offlineTransactions.forEach { offlineTx ->
                     val transaction = try {
                         json.decodeFromString<Transaction>(offlineTx.transactionJson)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        posDao.deleteOfflineTransaction(offlineTx)
                        return@forEach
                    }
                    
                        try {
                        // Try upload (scoped to App Scope so it doesn't die if UI dies, but cleaner than GlobalScope)
                        com.curbos.pos.common.Logger.d("TransactionSyncManager", "Uploading ${transaction.id}...")
                        
                        val result = SupabaseManager.uploadTransaction(transaction)
                        when (result) {
                            is Result.Success -> {
                                com.curbos.pos.common.Logger.d("TransactionSyncManager", "Upload success. Deleting offline record ${offlineTx.id}...")
                                posDao.deleteOfflineTransaction(offlineTx)
                                com.curbos.pos.common.Logger.d("TransactionSyncManager", "Deleted.")
                            }
                            is Result.Error -> {
                                // If it's a 409 conflict, it means it's already in Supabase
                                if (result.message?.contains("409") == true || 
                                    result.message?.contains("already exists") == true ||
                                    result.message?.contains("duplicate key") == true) {
                                    com.curbos.pos.common.Logger.w("TransactionSyncManager", "Conflict detected (already exists), removing local record.")
                                    posDao.deleteOfflineTransaction(offlineTx)
                                } else {
                                    // Keep in queue for real errors (timeout, network down)
                                    com.curbos.pos.common.Logger.e("TransactionSyncManager", "Sync failed for ${transaction.id}: ${result.message}")
                                    allSuccess = false
                                }
                            }
                            else -> { allSuccess = false }
                        }
                    } catch (e: Exception) {
                        // Handle exceptions thrown by Supabase client (e.g. UnknownRestException)
                        val msg = e.message ?: ""
                        if (msg.contains("duplicate key") || msg.contains("violates unique constraint")) {
                             com.curbos.pos.common.Logger.w("TransactionSyncManager", "Conflict detected via Exception (already exists), removing local record.")
                             posDao.deleteOfflineTransaction(offlineTx)
                        } else {
                            com.curbos.pos.common.Logger.e("TransactionSyncManager", "Exception during upload: ${e.message}", e)
                            e.printStackTrace()
                            allSuccess = false
                        }
                    }
                }

                // 2. Process Offline Customer Updates
                if (offlineCustomers.isNotEmpty()) {
                    com.curbos.pos.common.Logger.d("TransactionSyncManager", "Processing customer queue: ${offlineCustomers.size} items")
                    offlineCustomers.forEach { offlineCustomer ->
                        val customer = try {
                            json.decodeFromString<Customer>(offlineCustomer.customerJson)
                        } catch (e: Exception) {
                            posDao.deleteOfflineCustomerUpdate(offlineCustomer)
                            return@forEach
                        }

                        try {
                            com.curbos.pos.common.Logger.d("TransactionSyncManager", "Uploading customer ${customer.id}...")
                            val result = SupabaseManager.upsertCustomer(customer)
                            when (result) {
                                is Result.Success -> {
                                    com.curbos.pos.common.Logger.d("TransactionSyncManager", "Customer upload success. Deleting record ${offlineCustomer.id}...")
                                    posDao.deleteOfflineCustomerUpdate(offlineCustomer)
                                }
                                is Result.Error -> {
                                    if (result.message?.contains("duplicate key") == true || result.message?.contains("conflict") == true) {
                                        posDao.deleteOfflineCustomerUpdate(offlineCustomer)
                                    } else {
                                        com.curbos.pos.common.Logger.e("TransactionSyncManager", "Customer sync failed: ${result.message}")
                                        allSuccess = false
                                    }
                                }
                                else -> { allSuccess = false }
                            }
                        } catch (e: Exception) {
                            allSuccess = false
                        }
                    }
                }

                return allSuccess
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
    }
}
