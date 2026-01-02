package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "quests")
@Serializable
data class Quest(
    @PrimaryKey
    val id: String, // UUID
    
    val title: String,
    val description: String,
    
    @SerialName("quest_type")
    val questType: String, // BUY_X_ITEMS, SPEND_AMOUNT, VISIT_FREQUENCY
    
    @SerialName("target_category")
    val targetCategory: String? = null, // e.g., "Burrito", "Drinks" (if null, any item)
    
    @SerialName("required_count")
    val requiredCount: Int = 1, // e.g., 5 items, or $50
    
    @SerialName("reward_miles")
    val rewardMiles: Double,
    
    @SerialName("expiry_date")
    val expiryDate: Long? = null,
    
    @SerialName("is_active")
    val isActive: Boolean = true
)
