package com.curbos.pos.data

import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.MenuItem
import kotlinx.coroutines.flow.firstOrNull
import com.curbos.pos.data.model.LoyaltyReward
import com.curbos.pos.data.model.LoyaltyConstants
import java.util.UUID

class DataSeeder(private val posDao: PosDao) {

    suspend fun seedDataIfEmpty() {
        val currentItems = posDao.getAllMenuItems().firstOrNull()
        if (currentItems.isNullOrEmpty()) {
            val initialItems = listOf(
                // Tacos
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Al Pastor Elysium",
                    category = "Tacos",
                    price = 4.50,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = LoyaltyConstants.TacoRegion.OAXACA
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Carne Asada Supreme",
                    category = "Tacos",
                    price = 5.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = LoyaltyConstants.TacoRegion.JALISCO
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Baja Fish Nirvana",
                    category = "Tacos",
                    price = 5.50,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = LoyaltyConstants.TacoRegion.BAJA
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Vegan 'Chorizo' Dream",
                    category = "Tacos",
                    price = 4.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = LoyaltyConstants.TacoRegion.YUCATAN
                ),

                // Drinks
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Horchata Gold",
                    category = "Drinks",
                    price = 3.50,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = LoyaltyConstants.TacoRegion.CHEFS_VAULT
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Jarritos Lime",
                    category = "Drinks",
                    price = 3.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = null
                ),

                // Merch
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "CurbOS Cap",
                    category = "Merch",
                    price = 25.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = null
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Spicy Sauce Bottle",
                    category = "Merch",
                    price = 12.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true,
                    region = null
                )
            )
            posDao.insertMenuItems(initialItems)
        }

        val currentCustomers = posDao.getAllCustomers().firstOrNull()
        if (currentCustomers.isNullOrEmpty()) {
            val testCustomers = listOf(
                com.curbos.pos.data.model.Customer(
                    id = "c1",
                    fullName = "Johnny Quesadilla",
                    phoneNumber = "555-0101",
                    zipCode = "90210",
                    lifetimeMiles = 450.0,
                    redeemableMiles = 120.0,
                    currentRank = LoyaltyConstants.TacoRank.LEGEND.rankName,
                    email = "johnny@tacos.com",
                    unlockedRegions = listOf(LoyaltyConstants.TacoRegion.BAJA, LoyaltyConstants.TacoRegion.OAXACA)
                ),
                com.curbos.pos.data.model.Customer(
                    id = "c2",
                    fullName = "Maria Salsa",
                    phoneNumber = "555-0202",
                    zipCode = "10001",
                    lifetimeMiles = 890.0,
                    redeemableMiles = 45.0,
                    currentRank = LoyaltyConstants.TacoRank.MYTHIC.rankName,
                    email = "maria@mexico.com",
                    unlockedRegions = LoyaltyConstants.TacoRegion.ALL_REGIONS
                ),
                com.curbos.pos.data.model.Customer(
                    id = "c3",
                    fullName = "Burrito Bob",
                    phoneNumber = "555-0303",
                    zipCode = "80202",
                    lifetimeMiles = 120.0,
                    redeemableMiles = 20.0,
                    currentRank = LoyaltyConstants.TacoRank.REGULAR.rankName,
                    email = "bob@burrito.org"
                )
            )
            testCustomers.forEach { posDao.insertCustomer(it) }
        }

        val currentRewards = posDao.getAllLoyaltyRewards().firstOrNull()
        if (currentRewards.isNullOrEmpty()) {
            val initialRewards = listOf(
                LoyaltyReward(
                    id = "r1",
                    description = "Free Drug (50 Miles)",
                    costMiles = 50,
                    discountAmount = 3.50,
                    isAutoApplied = true
                ),
                LoyaltyReward(
                    id = "r2",
                    description = "Free Taco (120 Miles)",
                    costMiles = 120,
                    discountAmount = 5.00,
                    isAutoApplied = true
                ),
                LoyaltyReward(
                    id = "r3",
                    description = "VIP Merch (250 Miles)",
                    costMiles = 250,
                    discountAmount = 0.0,
                    isAutoApplied = false
                ),
                LoyaltyReward(
                    id = "r4",
                    description = "Bring-a-Friend (400 Miles)",
                    costMiles = 400,
                    discountAmount = 5.00,
                    isAutoApplied = true
                ),
                LoyaltyReward(
                    id = "r5",
                    description = "Legend Night (700 Miles)",
                    costMiles = 700,
                    discountAmount = 0.0,
                    isAutoApplied = false
                )
            )
            posDao.insertLoyaltyRewards(initialRewards)
        }
    }
}
