package com.curbos.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.data.model.OfflineTransaction
import com.curbos.pos.data.model.ModifierOption

@Database(entities = [MenuItem::class, Transaction::class, ModifierOption::class, OfflineTransaction::class], version = 5, exportSchema = false)
@androidx.room.TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) {
                    return existing
                }
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "curbos-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
