package com.curbos.pos.data.p2p

import kotlinx.serialization.Serializable

@Serializable
enum class MESSAGE_TYPE {
    SNAPSHOT,    // Full list of active orders
    ORDER_ADDED, // Single new order
    ORDER_UPDATED, // Single updated order
    STATUS_UPDATE, // Status change (Display -> Host)
    CART_UPDATE // Live cart update (Host -> Customer Display)
}

@Serializable
data class P2PMessage(
    val type: MESSAGE_TYPE,
    val payload: String // JSON string of Transaction or List<Transaction>
)
