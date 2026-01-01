package com.curbos.pos.data

import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.MenuItem
import kotlinx.coroutines.flow.firstOrNull
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
                    isAvailable = true
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Carne Asada Supreme",
                    category = "Tacos",
                    price = 5.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Baja Fish Nirvana",
                    category = "Tacos",
                    price = 5.50,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Vegan 'Chorizo' Dream",
                    category = "Tacos",
                    price = 4.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),

                // Drinks
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Horchata Gold",
                    category = "Drinks",
                    price = 3.50,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Jarritos Lime",
                    category = "Drinks",
                    price = 3.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),

                // Merch
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "CurbOS Cap",
                    category = "Merch",
                    price = 25.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                ),
                MenuItem(
                    id = UUID.randomUUID().toString(),
                    name = "Spicy Sauce Bottle",
                    category = "Merch",
                    price = 12.00,
                    imageUrl = null,
                    taxRate = 0.1,
                    isAvailable = true
                )
            )
            posDao.insertMenuItems(initialItems)
        }

        val currentCustomers = posDao.getAllCustomers()
        if (currentCustomers.isEmpty()) {
            val testCustomers = listOf(
                com.curbos.pos.data.model.Customer(
                    id = "c1",
                    fullName = "Johnny Quesadilla",
                    phoneNumber = "555-0101",
                    zipCode = "90210",
                    lifetimeMiles = 450.0,
                    redeemableMiles = 120.0,
                    currentRank = "Nacho Ninja",
                    email = "johnny@tacos.com"
                ),
                com.curbos.pos.data.model.Customer(
                    id = "c2",
                    fullName = "Maria Salsa",
                    phoneNumber = "555-0202",
                    zipCode = "10001",
                    lifetimeMiles = 890.0,
                    redeemableMiles = 45.0,
                    currentRank = "Salsa Skipper",
                    email = "maria@mexico.com"
                ),
                com.curbos.pos.data.model.Customer(
                    id = "c3",
                    fullName = "Burrito Bob",
                    phoneNumber = "555-0303",
                    zipCode = "80202",
                    lifetimeMiles = 1200.0,
                    redeemableMiles = 300.0,
                    currentRank = "Tacotopia Titan",
                    email = "bob@burrito.org"
                )
            )
            testCustomers.forEach { posDao.insertCustomer(it) }
        }
    }
}
