package com.curbos.pos.data.model

import java.util.UUID

data class CartItem(
    val menuItem: MenuItem,
    val uuid: String = UUID.randomUUID().toString(),
    val modifiers: List<ModifierOption> = emptyList()
) {
    val totalPrice: Double
        get() = menuItem.price + modifiers.sumOf { it.priceDelta }
}
