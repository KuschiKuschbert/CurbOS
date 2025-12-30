package com.curbos.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.ui.viewmodel.CustomerDisplayViewModel
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange

@Composable
fun CustomerDisplayScreen(
    viewModel: CustomerDisplayViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    // Keep Screen On
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    com.curbos.pos.ui.components.PulsatingBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.liveCart.isNotEmpty()) {
            LiveCartView(uiState.liveCart)
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                if (isWideScreen) {
                    // TABLET / DESKTOP: Side-by-Side
                    Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        PreparingColumn(
                            uiState = uiState, 
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        
                        VerticalDivider(
                            color = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp, vertical = 32.dp),
                            thickness = 2.dp
                        )
                        
                        ReadyColumn(
                            uiState = uiState, 
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                } else {
                    // PHONE: Vertical Stack
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        // Ready First! (More important) or Split?
                        // Let's do Preparing Top (Weight 1) -> Ready Bottom (Weight 1)
                        
                        PreparingColumn(
                            uiState = uiState, 
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        
                        HorizontalDivider(
                            color = Color.DarkGray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            thickness = 2.dp
                        )
                        
                        ReadyColumn(
                            uiState = uiState, 
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCartView(items: List<com.curbos.pos.data.model.TransactionItem>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "YOUR ORDER 🌮",
            color = ElectricLime,
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name, 
                                color = Color.White, 
                                fontSize = 24.sp, 
                                fontWeight = FontWeight.Bold
                            )
                            if (item.modifiers.isNotEmpty()) {
                                Text(
                                    item.modifiers.joinToString(", "),
                                    color = Color.Gray,
                                    fontSize = 18.sp
                                )
                            }
                        }
                        
                        Text(
                            java.text.NumberFormat.getCurrencyInstance().format(item.price),
                            color = ElectricLime,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val total = items.sumOf { it.price }
        Text(
            "TOTAL: ${java.text.NumberFormat.getCurrencyInstance().format(total)}",
             color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun PreparingColumn(uiState: com.curbos.pos.ui.viewmodel.CustomerDisplayUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "PREPARING 👨‍🍳",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
        )

        if (uiState.preparingOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders in queue", color = Color.Gray, fontSize = 20.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.preparingOrders) { transaction ->
                    CustomerOrderCard(transaction, isReady = false)
                }
            }
        }
    }
}

@Composable
fun ReadyColumn(uiState: com.curbos.pos.ui.viewmodel.CustomerDisplayUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            "READY TO PICKUP 🔔",
            color = ElectricLime,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
        )

        if (uiState.readyOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("All caught up!", color = Color.Gray, fontSize = 20.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.readyOrders) { transaction ->
                    CustomerOrderCard(transaction, isReady = true)
                }
            }
        }
    }
}


@Composable
fun CustomerOrderCard(transaction: Transaction, isReady: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) ElectricLime else Color(0xFF1A1A1A)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Order Number
            Text(
                text = "#${transaction.orderNumber}",
                color = if (isReady) Color.Black else Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            
            // Name
            if (!transaction.customerName.isNullOrBlank()) {
                 Text(
                    text = transaction.customerName.uppercase(),
                    color = if (isReady) Color.Black else Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
