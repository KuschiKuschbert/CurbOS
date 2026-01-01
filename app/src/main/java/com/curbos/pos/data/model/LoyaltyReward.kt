package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "loyalty_rewards")
@Serializable
data class LoyaltyReward(
    @PrimaryKey
    val id: String,
    val description: String,
    @SerialName("cost_miles")
    val costMiles: Int,
    @SerialName("discount_amount")
    val discountAmount: Double = 0.0,
    @SerialName("is_auto_applied")
    val isAutoApplied: Boolean = true
)
