package com.curbos.pos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // Menu Items
    @Query("SELECT * FROM menu_items WHERE deletedAt IS NULL")
    fun getAllMenuItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE category = :category AND deletedAt IS NULL")
    fun getMenuItemsByCategory(category: String): Flow<List<MenuItem>>

    // Modifiers
    @Query("SELECT * FROM modifier_options WHERE deletedAt IS NULL")
    fun getAllModifiers(): Flow<List<ModifierOption>>

    @Query("SELECT COUNT(*) FROM menu_items WHERE category = :category")
    suspend fun getMenuItemCountByCategory(category: String): Int

    @Query("UPDATE menu_items SET category = :newName WHERE category = :oldName")
    suspend fun updateCategoryName(oldName: String, newName: String)

    @Query("DELETE FROM menu_items WHERE category = :category")
    suspend fun deleteItemsByCategory(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModifier(modifier: ModifierOption)

    @Update
    suspend fun updateModifier(modifier: ModifierOption)

    @Query("UPDATE modifier_options SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteModifier(id: String, timestamp: String)

    @Query("SELECT * FROM modifier_options WHERE updatedAt > :since")
    suspend fun getModifiedModifiers(since: String): List<ModifierOption>

    @androidx.room.Delete
    suspend fun deleteModifier(modifier: ModifierOption)

    @Query("SELECT MAX(updatedAt) FROM menu_items")
    suspend fun getLatestMenuUpdate(): String?

    @Query("SELECT MAX(updatedAt) FROM modifier_options")
    suspend fun getLatestModifierUpdate(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItem)

    @Update
    suspend fun updateMenuItem(item: MenuItem)

    @Query("UPDATE menu_items SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteMenuItem(id: String, timestamp: String)

    @Query("SELECT * FROM menu_items WHERE updatedAt > :since")
    suspend fun getModifiedMenuItems(since: String): List<MenuItem>

    @androidx.room.Delete
    suspend fun deleteMenuItem(item: MenuItem)

    @Query("DELETE FROM menu_items")
    suspend fun clearMenuItems()

    // Transactions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE fulfillmentStatus != 'COMPLETED' ORDER BY timestamp ASC")
    fun getActiveTransactions(): Flow<List<Transaction>>

    // CRITICAL: Must count from 'offline_transactions' (the queue), NOT the main 'transactions' table.
    // The main table items are marked synced/unsynced but the queue is the source of truth for "Pending Uploads".
    @Query("SELECT count(*) FROM offline_transactions")
    fun getUnsyncedCount(): Flow<Int>

    // Daily Summary logic
    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay AND status = 'COMPLETED'")
    suspend fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC LIMIT :limit")
    fun getTransactionsByCustomerId(customerId: String, limit: Int = 50): Flow<List<Transaction>>

    // Offline Queue
    @Insert
    suspend fun insertOfflineTransaction(transaction: com.curbos.pos.data.model.OfflineTransaction)

    @Query("SELECT * FROM offline_transactions ORDER BY id ASC")
    suspend fun getOfflineTransactions(): List<com.curbos.pos.data.model.OfflineTransaction>

    @androidx.room.Delete
    suspend fun deleteOfflineTransaction(transaction: com.curbos.pos.data.model.OfflineTransaction)

    // Offline Customer Queue
    @Insert
    suspend fun insertOfflineCustomerUpdate(update: com.curbos.pos.data.model.OfflineCustomerUpdate)

    @Query("SELECT * FROM offline_customer_updates ORDER BY id ASC")
    suspend fun getOfflineCustomerUpdates(): List<com.curbos.pos.data.model.OfflineCustomerUpdate>

    @androidx.room.Delete
    suspend fun deleteOfflineCustomerUpdate(update: com.curbos.pos.data.model.OfflineCustomerUpdate)

    @Query("SELECT MAX(orderNumber) FROM transactions WHERE timestamp >= :startOfDay")
    suspend fun getTodayMaxOrderNumber(startOfDay: Long): Int?

    // Customers
    @Query("SELECT * FROM customers WHERE phoneNumber = :phone")
    suspend fun getCustomerByPhone(phone: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Query("SELECT * FROM customers ORDER BY fullName ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers ORDER BY fullName ASC")
    suspend fun getCustomerList(): List<Customer>

    @Query("SELECT * FROM customers WHERE fullName LIKE :query OR phoneNumber LIKE :query ORDER BY fullName ASC")
    fun searchCustomersByName(query: String): Flow<List<Customer>>

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("SELECT MAX(updatedAt) FROM customers")
    suspend fun getLatestCustomerUpdate(): String?

    @Query("SELECT * FROM customers WHERE updatedAt > :since")
    suspend fun getModifiedCustomers(since: String): List<Customer>

    @Query("UPDATE customers SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCustomer(id: String, timestamp: String)

    // Loyalty Rewards
    @Query("SELECT * FROM loyalty_rewards")
    fun getAllLoyaltyRewards(): Flow<List<LoyaltyReward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyRewards(rewards: List<LoyaltyReward>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoyaltyReward(reward: LoyaltyReward)

    @Query("SELECT MAX(updatedAt) FROM loyalty_rewards")
    suspend fun getLatestRewardUpdate(): String?

    @Query("UPDATE loyalty_rewards SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteReward(id: String, timestamp: String)

    // Quests
    @Query("SELECT * FROM quests WHERE isActive = 1")
    fun getActiveQuests(): Flow<List<com.curbos.pos.data.model.Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<com.curbos.pos.data.model.Quest>)

    @Update
    suspend fun updateQuest(quest: com.curbos.pos.data.model.Quest)

    @Query("SELECT MAX(updatedAt) FROM quests")
    suspend fun getLatestQuestUpdate(): String?

    @Query("UPDATE quests SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteQuest(id: String, timestamp: String)
}
