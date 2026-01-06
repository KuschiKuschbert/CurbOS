package com.curbos.pos.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.theme.SecondaryText

/**
 * Dialog for selecting payment method (Card or Cash).
 * Displays surcharge calculation for card payments.
 */
@Composable
fun PaymentSelectionDialog(
    totalAmount: Double,
    onPaymentSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val surcharge = totalAmount * 0.022
    val cardTotal = totalAmount + surcharge

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CurbOSShapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ElectricLime
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                    Text("$%.2f".format(totalAmount), style = MaterialTheme.typography.bodyLarge, color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card Surcharge (1.6%):", style = MaterialTheme.typography.bodyLarge, color = SecondaryText)
                    Text("$%.2f".format(surcharge), style = MaterialTheme.typography.bodyLarge, color = SecondaryText)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SecondaryText)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Card Total:", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("$%.2f".format(cardTotal), style = MaterialTheme.typography.titleLarge, color = ElectricLime)
                }

                Spacer(modifier = Modifier.height(32.dp))

                val buttonModifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp) 

                Button(
                    onClick = { onPaymentSelected("CARD") },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                    modifier = buttonModifier,
                    shape = CurbOSShapes.medium
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Pay by card") 
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("PAY CARD ($%.2f)".format(cardTotal), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPaymentSelected("CASH") },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                    modifier = buttonModifier,
                    shape = CurbOSShapes.medium
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PAY CASH ($%.2f)".format(totalAmount), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        }
    }
}
