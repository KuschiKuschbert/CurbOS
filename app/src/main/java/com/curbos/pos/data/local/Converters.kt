package com.curbos.pos.data.local

import androidx.room.TypeConverter
import com.curbos.pos.data.model.TransactionItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTransactionItemList(value: List<TransactionItem>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toTransactionItemList(value: String): List<TransactionItem> {
        return json.decodeFromString(value)
    }
}
