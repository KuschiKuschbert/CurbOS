package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Entity(tableName = "customers")
@Serializable
data class Customer(
    @PrimaryKey
    val id: String, // UUID
    @SerialName("phone_number")
    val phoneNumber: String,
    @SerialName("full_name")
    val fullName: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("lifetime_miles")
    val lifetimeMiles: Double = 0.0,
    @SerialName("redeemable_miles")
    val redeemableMiles: Double = 0.0,
    @SerialName("current_rank")
    val currentRank: String = "Street Rookie",
    @SerialName("zip_code")
    val zipCode: String? = null,
    @SerialName("unlocked_regions")
    val unlockedRegions: List<String> = emptyList(),
    
    // --- New Loyalty Fields ---
    @SerialName("streak_count")
    val streakCount: Int = 0,
    
    @SerialName("last_visit")
    val lastVisit: Long = 0L,
    
    @SerialName("stamp_cards")
    val stampCards: Map<String, Int> = emptyMap(), // Category -> Count
    
    @SerialName("active_quests")
    val activeQuests: List<QuestProgress> = emptyList(),

    @SerialName("updated_at")
    val updatedAt: String? = null,
    
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

@Serializable
data class QuestProgress(
    @SerialName("quest_id") val questId: String,
    @SerialName("current_value") val currentValue: Int,
    @SerialName("is_completed") val isCompleted: Boolean = false
)
