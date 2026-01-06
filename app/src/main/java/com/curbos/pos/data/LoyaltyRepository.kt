package com.curbos.pos.data

import com.curbos.pos.data.model.LoyaltyConfig
import com.curbos.pos.data.model.LoyaltyConstants
import com.curbos.pos.data.model.LoyaltyRankConfig
import com.curbos.pos.data.model.StampCardConfig
import com.curbos.pos.data.remote.SupabaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.curbos.pos.common.Result

object LoyaltyRepository {

    private val _config = MutableStateFlow<LoyaltyConfig?>(null)
    val config: StateFlow<LoyaltyConfig?> = _config

    // Fallback defaults from Constants if config is missing
    private val defaultRanks = LoyaltyConstants.TacoRank.entries.map { 
        LoyaltyRankConfig(it.name, it.rankName, it.minMiles)
    }
    
    // Default stamps are hardcoded in code previously, here we expose them too
    private val defaultStamps = listOf(
        StampCardConfig("Tacos", 10),
        StampCardConfig("Drinks", 10)
    )

    suspend fun refreshConfig() {
        when (val result = SupabaseManager.fetchLoyaltyConfig()) {
            is Result.Success -> {
                _config.value = result.data
            }
            is Result.Error -> {
                // Keep existing or defaults
                com.curbos.pos.common.Logger.e("LoyaltyRepo", "Failed to refresh config")
            }
            else -> {}
        }
    }
    
    fun getRankForMiles(miles: Double): String {
        val currentConfig = _config.value
        val ranks = currentConfig?.ranks?.takeIf { it.isNotEmpty() } ?: defaultRanks
        
        // Sort by miles desc to find highest matching
        val sorted = ranks.sortedByDescending { it.minMiles }
        return sorted.find { miles >= it.minMiles }?.name ?: "Street Rookie"
    }
    
    fun getNextRank(currentMiles: Double): LoyaltyRankConfig? {
        val currentConfig = _config.value
        val ranks = currentConfig?.ranks?.takeIf { it.isNotEmpty() } ?: defaultRanks
        
        return ranks.sortedBy { it.minMiles }
            .firstOrNull { it.minMiles > currentMiles }
    }
    
    fun getMaxStampsForCategory(category: String): Int {
         val currentConfig = _config.value
         val stamps = currentConfig?.stamps?.takeIf { it.isNotEmpty() } ?: defaultStamps
         
         return stamps.find { it.category.equals(category, ignoreCase = true) }?.maxStamps ?: 10
    }

    fun getRankByName(name: String): LoyaltyRankConfig? {
        val currentConfig = _config.value
        val ranks = currentConfig?.ranks?.takeIf { it.isNotEmpty() } ?: defaultRanks
        return ranks.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getAvailableRegions(): List<String> {
        val currentConfig = _config.value
        // If config has regions, use them. Otherwise fallback to Constants.
        return currentConfig?.regions?.takeIf { it.isNotEmpty() } ?: LoyaltyConstants.TacoRegion.ALL_REGIONS
    }
}
