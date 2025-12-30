package com.curbos.pos.ui.screens

import com.curbos.pos.data.remote.SupabaseManager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.curbos.pos.data.CsvExportManager
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.ui.theme.ElectricGradient
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.theme.DarkSurfaceGradient
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import org.json.JSONArray

@Composable
fun AdminScreen(
    viewModel: com.curbos.pos.ui.viewmodel.AdminViewModel,
    csvExportManager: CsvExportManager? = null,
    onLaunchCustomerDisplay: () -> Unit = {},
    onLaunchP2PSetup: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    
    // Derived State
    val transactions = uiState.transactions
    val totalRevenue = uiState.totalRevenue
    val totalTx = uiState.totalTx
    val hourlySales = remember(transactions) { calculateHourlySales(transactions) }
    val bestSellers = remember(transactions) { calculateBestSellers(transactions) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Text(
            text = "COMMAND CENTER",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                brush = ElectricGradient
            ),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(color = ElectricLime)
        } else {
            // --- KPI CARDS ---
            Row(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                KpiCard(
                    title = "Total Revenue",
                    value = "$%.2f".format(totalRevenue),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                KpiCard(
                    title = "Transactions",
                    value = totalTx.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- RUSH HOUR CHART ---
            Text("Rush Hour (Sales by Hour)", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            HourlySalesChart(hourlySales = hourlySales, modifier = Modifier.fillMaxWidth().height(200.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // --- BEST SELLERS ---
            Text("Top Movers", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            bestSellers.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${index + 1}. ${pair.first}", color = Color.White)
                    Text("${pair.second} sold", color = ElectricLime)
                }
                HorizontalDivider(color = Color.DarkGray)
            }
            
            if (bestSellers.isEmpty()) {
                Text("No sales yet today.", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // --- UTILITIES ---


            Button(
                onClick = {
                    scope.launch {
                        csvExportManager?.exportDailySales()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Export Daily CSV Report", color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    viewModel.syncMenu()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime)
            ) {
                Text("SYNC MENU FROM CLOUD ☁️", color = Color.Black)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.forceSyncOrders()
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("PUSH PENDING ORDERS 📤", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLaunchP2PSetup,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                 Text("OFFLINE P2P SETUP 📶", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLaunchCustomerDisplay,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C))
            ) {
                Text("LAUNCH CUSTOMER DISPLAY \uD83D\uDCFA", color = ElectricLime, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- SYSTEM CONFIGURATION ---
            Text("System Configuration", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.webBaseUrl,
                onValueChange = { viewModel.updateWebBaseUrl(it) },
                label = { Text("Web Portal URL (for QR codes)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // --- APP UPDATE ---
            Text("App Updates", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Update System", color = Color.White, fontWeight = FontWeight.Bold)
                            val currentVersion = com.curbos.pos.BuildConfig.VERSION_NAME
                            Text("Current Version: $currentVersion", color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        if (uiState.isUpdateAvailable) {
                            Button(
                                onClick = { viewModel.installUpdate() },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                            ) {
                                Text("UPDATE NOW")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.checkForUpdates() }
                            ) {
                                Text("CHECK FOR UPDATE")
                            }
                        }
                    }
                    
                    if (uiState.isUpdateAvailable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New version ${uiState.latestRelease?.tagName} available!",
                            color = SafetyOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // --- SETTINGS ---
            Text("Settings", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simplified Kitchen Flow", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Skip 'Start Cooking'. Orders go directly to Ready.", color = Color.Gray, fontSize = 12.sp)
                }
                Switch(
                    checked = uiState.isSimplifiedKds,
                    onCheckedChange = { viewModel.toggleSimplifiedKds(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = ElectricLime
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // --- DANGER ZONE ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFAA0000).copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                     Text("DANGER ZONE", style = MaterialTheme.typography.titleMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     Button(
                        onClick = { viewModel.resetToDemoData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                     ) {
                         Text("RESET TO DEMO DATA")
                     }
                     
                     Spacer(modifier = Modifier.height(8.dp))
                     
                     OutlinedButton(
                        onClick = { viewModel.clearAllData() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                     ) {
                         Text("CLEAR ALL DATA")
                     }
                }
            }
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = ElectricLime, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
    }
}

@Composable
fun HourlySalesChart(hourlySales: Map<Int, Double>, modifier: Modifier = Modifier) {
    val maxSales = hourlySales.values.maxOrNull() ?: 1.0
    
    Canvas(modifier = modifier.background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))) {
        val barWidth = size.width / 24f
        val maxBarHeight = size.height 

        for (hour in 0..23) {
            val sales = hourlySales[hour] ?: 0.0
            val barHeight = (sales / maxSales) * maxBarHeight
            
            if (sales > 0) {
                drawRect(
                    brush = ElectricGradient,
                    topLeft = Offset(x = hour * barWidth, y = (size.height - barHeight).toFloat()),
                    size = Size(width = barWidth - 4f, height = barHeight.toFloat()) // -4f for spacing
                )
            }
        }
    }
}

// --- LOGIC HELPERS ---

fun calculateHourlySales(transactions: List<Transaction>): Map<Int, Double> {
    return transactions.groupBy { 
        Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).hour 
    }.mapValues { entry -> 
        entry.value.sumOf { it.totalAmount } 
    }
}

fun calculateBestSellers(transactions: List<Transaction>): List<Pair<String, Int>> {
    val itemCounts = mutableMapOf<String, Int>()
    
    transactions.forEach { tx ->
        tx.items.forEach { item ->
            itemCounts[item.name] = (itemCounts[item.name] ?: 0) + item.quantity
        }
    }
    
    return itemCounts.toList().sortedByDescending { it.second }.take(5)
}
