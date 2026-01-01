package com.curbos.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import java.util.Date

@Entity(tableName = "transactions")
@Serializable
data class Transaction(
    @PrimaryKey
    val id: String, // UUID
    @kotlinx.serialization.SerialName("timestamp")
    val timestamp: Long,
    @kotlinx.serialization.SerialName("total_amount")
    val totalAmount: Double,
    @kotlinx.serialization.SerialName("tax_amount")
    val taxAmount: Double,
    @Serializable(with = StringifiedItemsSerializer::class)
    @kotlinx.serialization.SerialName("items_json")
    val items: List<TransactionItem>, 
    @kotlinx.serialization.SerialName("status")
    val status: String, // PENDING, COMPLETED, CANCELLED
    @kotlinx.serialization.SerialName("payment_method")
    val paymentMethod: String, // CASH, CARD
    @kotlinx.serialization.SerialName("fulfillment_status")
    val fulfillmentStatus: String = "PENDING",
    @kotlinx.serialization.SerialName("order_number")
    val orderNumber: Int? = null,
    @kotlinx.serialization.SerialName("customer_name")
    val customerName: String? = null,
    
    @kotlinx.serialization.SerialName("discount_amount")
    val discountAmount: Double = 0.0,

    @kotlinx.serialization.SerialName("promo_code")
    val promoCode: String? = null,

    @kotlinx.serialization.SerialName("customer_id")
    val customerId: String? = null,

    @kotlinx.serialization.SerialName("miles_earned")
    val milesEarned: Double = 0.0,

    @kotlinx.serialization.SerialName("miles_redeemed")
    val milesRedeemed: Double = 0.0,

    @kotlinx.serialization.Transient
    @kotlinx.serialization.SerialName("square_transaction_id")
    val squareTransactionId: String? = null,
    
    @kotlinx.serialization.Transient
    val isSynced: Boolean = false
)

@Serializable
data class TransactionItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val modifiers: List<String> = emptyList(),
    val isCompleted: Boolean = false
)

object StringifiedItemsSerializer : kotlinx.serialization.KSerializer<List<TransactionItem>> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor = 
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("StringifiedItems", kotlinx.serialization.descriptors.PrimitiveKind.STRING)

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: List<TransactionItem>) {
        val jsonString = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(TransactionItem.serializer()), value)
        encoder.encodeString(jsonString)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): List<TransactionItem> {
        val jsonString = decoder.decodeString()
        return json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(TransactionItem.serializer()), jsonString)
    }
}


