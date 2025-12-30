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
    }
}
