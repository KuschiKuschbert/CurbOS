package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "offline_transactions")
@Serializable
data class OfflineTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionJson: String, // Serialize the full Transaction object here
    val timestamp: Long = System.currentTimeMillis()
)
