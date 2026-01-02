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

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringIntMap(value: String): Map<String, Int> {
        return json.decodeFromString(value)
    }

    @TypeConverter
    fun fromQuestProgressList(value: List<com.curbos.pos.data.model.QuestProgress>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toQuestProgressList(value: String): List<com.curbos.pos.data.model.QuestProgress> {
        return json.decodeFromString(value)
    }
}
