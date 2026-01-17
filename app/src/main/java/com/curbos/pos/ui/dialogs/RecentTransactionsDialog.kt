package com.curbos.pos.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.curbos.pos.R
import androidx.compose.ui.window.Dialog
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SecondaryText
import com.curbos.pos.ui.theme.SurfaceColor

/**
 * Dialog showing recent transactions that can be reopened.
 */
@Composable
fun RecentTransactionsDialog(
    transactions: List<Transaction>,
    onTransactionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CurbOSShapes.extraLarge,
            color = SurfaceColor,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.dialog_recent_orders_title), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.content_desc_close_dialog), tint = SecondaryText)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.dialog_recent_orders_empty), color = SecondaryText)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(transactions) { tx ->
                            RecentTransactionItem(tx, onClick = { onTransactionClick(tx.id) })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = CurbOSShapes.medium
                ) {
                    Text(stringResource(R.string.dialog_close), color = Color.White)
                }
            }
        }
    }
}

/**
 * Individual transaction item in the recent transactions list.
 */
@Composable
fun RecentTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = CurbOSShapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${transaction.orderNumber} - ${transaction.customerName ?: stringResource(R.string.dialog_recent_orders_guest)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.paymentMethod} • ${"%.2f".format(transaction.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
            Icon(
                Icons.Default.History, 
                contentDescription = stringResource(R.string.content_desc_reopen_order, transaction.orderNumber ?: 0), 
                tint = ElectricLime.copy(alpha = 0.5f)
            )
        }
    }
}
