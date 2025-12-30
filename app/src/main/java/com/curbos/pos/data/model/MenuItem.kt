package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "menu_items")
@Serializable
data class MenuItem(
    @PrimaryKey
    val id: String, // UUID from Supabase
    val name: String,
    val category: String,
    val price: Double,
    @kotlinx.serialization.SerialName("image_url")
    val imageUrl: String?,
    @kotlinx.serialization.SerialName("tax_rate")
    val taxRate: Double = 0.1, // Default GST 10%
    @kotlinx.serialization.SerialName("is_available")
    val isAvailable: Boolean = true,
    @kotlinx.serialization.SerialName("updated_at")
    val updatedAt: String? = null
)
