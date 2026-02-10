package com.curbos.pos.ui.screens

import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.ui.viewmodel.AdminIntent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.curbos.pos.R
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.curbos.pos.data.CsvExportManager
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.DividerColor
import com.curbos.pos.ui.theme.ElectricGradient
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.ErrorRed
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.theme.SecondaryText
import com.curbos.pos.ui.theme.SurfaceColor
import java.time.Instant
import java.time.ZoneId

@Composable
fun AdminScreen(
    viewModel: com.curbos.pos.ui.viewmodel.AdminViewModel,
    csvExportManager: CsvExportManager? = null,
    onLaunchP2PSetup: () -> Unit = {},
    onNavigateToMenuCatalog: () -> Unit = {},
    onNavigateToModifiers: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {}
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
        AdminHeaderSection()

        if (uiState.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = ElectricLime)
            }
        } else {
            // --- METRICS ---
            MetricsSection(totalRevenue, totalTx)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- QUICK ACTIONS GRID ---
            Text(stringResource(R.string.admin_quick_actions), style = MaterialTheme.typography.titleMedium, color = SecondaryText)
            Spacer(modifier = Modifier.height(16.dp))
            
            ActionGrid(
                onMenuCatalog = onNavigateToMenuCatalog,
                onModifiers = onNavigateToModifiers,
                onSync = { viewModel.onIntent(AdminIntent.SyncMenu) },
                onPushOrders = { viewModel.onIntent(AdminIntent.ForceSyncOrders) },
                onExportCsv = { scope.launch { csvExportManager?.exportDailySales() } },
                onP2P = onLaunchP2PSetup,
                onCustomers = onNavigateToCustomers
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- INSIGHTS ---
            Text(stringResource(R.string.admin_insights_title), style = MaterialTheme.typography.titleMedium, color = SecondaryText)
            Spacer(modifier = Modifier.height(16.dp))
            HourlySalesChart(hourlySales = hourlySales, modifier = Modifier.fillMaxWidth().height(180.dp))
            Spacer(modifier = Modifier.height(16.dp))
            BestSellersCard(bestSellers)
            
            Spacer(modifier = Modifier.height(32.dp))

            // --- SETTINGS LIST ---
            Text(stringResource(R.string.admin_system_settings), style = MaterialTheme.typography.titleMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = CurbOSShapes.large
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Web URL
                    SettingItem {
                         OutlinedTextField(
                            value = uiState.webBaseUrl,
                            onValueChange = { viewModel.onIntent(AdminIntent.UpdateWebBaseUrl(it)) },
                            label = { Text(stringResource(R.string.admin_setting_web_url)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                    HorizontalDivider(color = DividerColor)
                    
                    // Kitchen Flow
                    SettingSwitchItem(
                        title = stringResource(R.string.admin_setting_kitchen_flow),
                        subtitle = stringResource(R.string.admin_setting_kitchen_flow_subtitle),
                        checked = uiState.isSimplifiedKds,
                        onCheckedChange = { viewModel.onIntent(AdminIntent.ToggleSimplifiedKds(it)) }
                    )
                    HorizontalDivider(color = DividerColor)

                    // Developer Mode
                    SettingSwitchItem(
                        title = stringResource(R.string.admin_setting_dev_mode),
                        subtitle = stringResource(R.string.admin_setting_dev_mode_subtitle),
                        checked = uiState.isDeveloperMode,
                        onCheckedChange = { viewModel.onIntent(AdminIntent.ToggleDeveloperMode(it)) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // --- UPDATE CARD ---
            UpdateCard(
                currentVersion = com.curbos.pos.BuildConfig.VERSION_NAME,
                isUpdateAvailable = uiState.isUpdateAvailable,
                latestVersion = uiState.latestRelease?.tagName,
                downloadProgress = uiState.downloadProgress,
                onCheck = { viewModel.onIntent(AdminIntent.CheckForUpdates) },
                onUpdate = { viewModel.onIntent(AdminIntent.InstallUpdate) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            // --- DANGER ZONE ---
            DangerZone(
                onResetDemo = { viewModel.onIntent(AdminIntent.ResetToDemoData) },
                onClearAll = { viewModel.onIntent(AdminIntent.ClearAllData) }
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// --- COMPONENTS ---

@Composable
fun AdminHeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
             Text(
                text = stringResource(R.string.admin_header_dashboard),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = stringResource(R.string.admin_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = ElectricLime
            )
        }
    }
}

@Composable
fun MetricsSection(revenue: Double, orders: Int) {
    Row(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        // Revenue Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
             modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
             Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.admin_card_revenue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.7f))
                Text("$%.2f".format(revenue), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Orders Card
        Card(
             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
             modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
             Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {

                Text(stringResource(R.string.nav_orders), style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                Text(orders.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ActionGrid(
    onMenuCatalog: () -> Unit,
    onModifiers: () -> Unit,
    onSync: () -> Unit,
    onPushOrders: () -> Unit,
    onExportCsv: () -> Unit,
    onP2P: () -> Unit,
    onCustomers: () -> Unit
) {
    // 2-Column Grid Layout manual implementation for scroll compatibility
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
             ActionCard(
                 title = stringResource(R.string.admin_action_menu_catalog), 
                 icon = Icons.AutoMirrored.Filled.MenuBook, 
                 color = ElectricLime, 
                 onClick = onMenuCatalog, 
                 modifier = Modifier.weight(1f)
             )
             ActionCard(
                 title = stringResource(R.string.admin_action_modifiers), 
                 icon = Icons.Filled.Edit, 
                 color = MaterialTheme.colorScheme.tertiary, 
                 onClick = onModifiers, 
                 modifier = Modifier.weight(1f)
             )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
             ActionCard(
                 title = stringResource(R.string.admin_action_sync_menu), 
                 icon = Icons.Filled.CloudDownload, 
                 color = MaterialTheme.colorScheme.surfaceVariant, 
                 textColor = Color.White,
                 onClick = onSync, 
                 modifier = Modifier.weight(1f)
             )
             ActionCard(
                 title = stringResource(R.string.admin_action_push_orders), 
                 icon = Icons.Filled.CloudUpload, 
                 color = MaterialTheme.colorScheme.surfaceVariant, 
                 textColor = Color.White,
                 onClick = onPushOrders, 
                 modifier = Modifier.weight(1f)
             )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
             ActionCard(
                 title = stringResource(R.string.admin_action_export_csv), 
                 icon = Icons.Filled.TableChart, 
                 color = MaterialTheme.colorScheme.surfaceVariant, 
                 textColor = Color.White,
                 onClick = onExportCsv, 
                 modifier = Modifier.weight(1f)
             )
             ActionCard(
                 title = stringResource(R.string.admin_action_p2p), 
                 icon = Icons.Filled.Share, 
                 color = MaterialTheme.colorScheme.surfaceVariant, 
                 textColor = Color.White,
                 onClick = onP2P, 
                 modifier = Modifier.weight(1f)
             )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
             ActionCard(
                 title = stringResource(R.string.admin_action_customers), 
                 icon = Icons.Rounded.People, 
                 color = MaterialTheme.colorScheme.primaryContainer, 
                 textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                 onClick = onCustomers, 
                 modifier = Modifier.weight(1f)
             )
             Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ActionCard(
    title: String, 
    icon: ImageVector, 
    color: Color, 
    textColor: Color = Color.Black,
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title, tint = textColor, modifier = Modifier.size(28.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun BestSellersCard(bestSellers: List<Pair<String, Int>>) {
     Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
             Text(stringResource(R.string.admin_top_movers), style = MaterialTheme.typography.titleSmall, color = SecondaryText)
             Spacer(modifier = Modifier.height(12.dp))
             
             if (bestSellers.isEmpty()) {
                 Text(stringResource(R.string.admin_no_sales_data), style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
             } else {
                 bestSellers.forEachIndexed { index, pair ->
                     Row(
                         modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                         horizontalArrangement = Arrangement.SpaceBetween
                     ) {
                         Text("${index + 1}. ${pair.first}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                         Text("${pair.second}", color = ElectricLime, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                     }
                     if (index < bestSellers.size - 1) {
                        HorizontalDivider(color = Color.White.copy(alpha=0.1f))
                     }
                 }
             }
        }
     }
}

@Composable
fun SettingItem(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        content()
    }
}

@Composable
fun SettingSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Medium)
            Text(subtitle, color = SecondaryText, style = MaterialTheme.typography.labelSmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = ElectricLime
            )
        )
    }
}

@Composable
fun DangerZone(onResetDemo: () -> Unit, onClearAll: () -> Unit) {
     Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1515)), // Dark red bg
        shape = CurbOSShapes.large
     ) {
         Column(Modifier.padding(16.dp)) {
             Text(stringResource(R.string.admin_danger_zone), color = ErrorRed, fontWeight = FontWeight.Bold)
             Spacer(modifier = Modifier.height(16.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                 OutlinedButton(
                     onClick = onResetDemo, 
                     modifier = Modifier.weight(1f),
                     colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                 ) {
                     Text(stringResource(R.string.admin_reset_demo))
                 }
                 OutlinedButton(
                     onClick = onClearAll,
                     modifier = Modifier.weight(1f),
                     colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                 ) {
                     Text(stringResource(R.string.admin_clear_all))
                 }
             }
         }
     }
}

@Composable
fun UpdateCard(
    currentVersion: String,
    isUpdateAvailable: Boolean,
    latestVersion: String?,
    downloadProgress: Int,
    onCheck: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = if (isUpdateAvailable) androidx.compose.foundation.BorderStroke(1.dp, SafetyOrange) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (isUpdateAvailable) stringResource(R.string.update_available_fmt, latestVersion ?: "") else stringResource(R.string.update_up_to_date),
                    color = if (isUpdateAvailable) SafetyOrange else Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.update_version_fmt, currentVersion), color = SecondaryText, style = MaterialTheme.typography.labelSmall)
                
                if (downloadProgress > 0) {
                     Spacer(modifier = Modifier.height(8.dp))
                     LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        color = SafetyOrange,
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                     )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (isUpdateAvailable) {
                 Button(onClick = onUpdate, colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)) {
                     Text(stringResource(R.string.action_update))
                 }
            } else {
                 TextButton(onClick = onCheck) { Text(stringResource(R.string.action_check)) }
            }
        }
    }
}


@Composable
fun HourlySalesChart(hourlySales: Map<Int, Double>, modifier: Modifier = Modifier) {
    val maxSales = hourlySales.values.maxOrNull() ?: 1.0
    
    Canvas(modifier = modifier.background(Color.Black.copy(alpha = 0.3f), CurbOSShapes.medium)) {
        val barWidth = size.width / 24f
        val maxBarHeight = size.height 
        val barSpacing = 4f

        for (hour in 0..23) {
            val sales = hourlySales[hour] ?: 0.0
            val barHeight = (sales / maxSales) * maxBarHeight
            
            if (sales > 0) {
                drawRect(
                    brush = ElectricGradient,
                    topLeft = Offset(x = hour * barWidth, y = (size.height - barHeight).toFloat()),
                    size = Size(width = barWidth - barSpacing, height = barHeight.toFloat()) 
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
