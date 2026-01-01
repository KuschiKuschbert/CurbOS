package com.curbos.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.model.OfflineTransaction
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward

@Database(entities = [MenuItem::class, Transaction::class, ModifierOption::class, OfflineTransaction::class, Customer::class, LoyaltyReward::class], version = 6, exportSchema = true)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    // Singleton logic removed as we use Hilt for injection.
    // See AppModule.kt for the real instance creation ("curbos-pos-db").
}
