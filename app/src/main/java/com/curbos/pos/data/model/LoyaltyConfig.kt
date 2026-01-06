package com.curbos.pos.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LoyaltyConfig(
    @SerialName("ranks")
    val ranks: List<LoyaltyRankConfig> = emptyList(),
    @SerialName("stamps")
    val stamps: List<StampCardConfig> = emptyList(),
    @SerialName("regions")
    val regions: List<String> = emptyList()
)

@Serializable
data class LoyaltyRankConfig(
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String,
    @SerialName("minMiles")
    val minMiles: Int
)

@Serializable
data class StampCardConfig(
    @SerialName("category")
    val category: String,
    @SerialName("maxStamps")
    val maxStamps: Int
)
