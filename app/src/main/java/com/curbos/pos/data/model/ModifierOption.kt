package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "modifier_options")
@Serializable
data class ModifierOption(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @kotlinx.serialization.SerialName("price_delta")
    val priceDelta: Double,
    val type: String = "ADDON", // "ADDON", "REMOVAL"
    val category: String? = null,
    @kotlinx.serialization.SerialName("is_available")
    val isAvailable: Boolean = true,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String? = null
)
