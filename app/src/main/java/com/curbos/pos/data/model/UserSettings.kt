package com.curbos.pos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("chef_name")
    val chefName: String? = null,
    @SerialName("simplified_kds")
    val simplifiedKds: Boolean = false
)
