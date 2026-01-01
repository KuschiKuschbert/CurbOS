package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "offline_customer_updates")
@Serializable
data class OfflineCustomerUpdate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
