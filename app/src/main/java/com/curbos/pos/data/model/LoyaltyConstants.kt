package com.curbos.pos.data.model

object LoyaltyConstants {
    enum class TacoRank(val rankName: String, val minMiles: Int, val maxMiles: Int) {
        STREET_ROOKIE("Street Rookie", 0, 50),
        REGULAR("Regular", 51, 150),
        OG("OG", 151, 350),
        LEGEND("Legend", 351, 700),
        MYTHIC("Mythic", 701, Int.MAX_VALUE);

        companion object {
            fun fromMiles(miles: Double): TacoRank {
                val milesInt = miles.toInt()
                return entries.find { milesInt in it.minMiles..it.maxMiles } ?: MYTHIC
            }
        }
    }

    object TacoRegion {
        const val BAJA = "Baja"
        const val OAXACA = "Oaxaca"
        const val YUCATAN = "Yucatán"
        const val JALISCO = "Jalisco"
        const val CHEFS_VAULT = "Chef's Vault"

        val ALL_REGIONS = listOf(BAJA, OAXACA, YUCATAN, JALISCO, CHEFS_VAULT)
    }

    enum class BonusAction(val title: String, val miles: Int, val iconEmoji: String) {
        BRING_A_FRIEND("Refer a Friend", 500, "👯"),
        SOCIAL_SHARE("Social Share", 100, "📸"),
        VIP_MERCH("VIP Merch", 1000, "👕")
    }
}
