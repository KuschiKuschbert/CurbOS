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
    val unlockedRegions: List<String> = emptyList()
)
