package com.curbos.pos.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class UserSettings(
    @SerialName("user_id") val userId: String,
    @SerialName("chef_name") val chefName: String?,
    @SerialName("simplified_kds") val simplifiedKds: Boolean = false,
    @SerialName("loyalty_config") val loyaltyConfig: LoyaltyConfig? = null
)
