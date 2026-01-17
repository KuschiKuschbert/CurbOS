package com.curbos.pos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.curbos.pos.R
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.ui.viewmodel.KitchenViewModel
import com.curbos.pos.ui.theme.CurbOSShapes
import com.curbos.pos.ui.theme.DarkBackground
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.theme.SecondaryText
import com.curbos.pos.ui.theme.SurfaceElevated
import java.util.concurrent.TimeUnit


@Composable
fun KitchenScreen(
    viewModel: KitchenViewModel,
    onExitKitchenMode: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAllDayView by remember { mutableStateOf(false) }

    // Keep Screen On
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Refresh data and settings when entering the screen
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    com.curbos.pos.ui.components.PulsatingBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- HEADER ---
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricLime)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.kitchen_error_loading), color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.error ?: stringResource(R.string.kitchen_error_unknown), color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }, colors = ButtonDefaults.buttonColors(containerColor = ElectricLime)) {
                            Text(stringResource(R.string.action_retry), color = Color.Black)
                        }
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val screenWidth = maxWidth
                    val minCardWidth = if (screenWidth < 600.dp) screenWidth else 300.dp

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = if (isAllDayView) 200.dp else minCardWidth),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stringResource(R.string.kitchen_header_title), 
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp 
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Button(
                                        onClick = { isAllDayView = !isAllDayView },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAllDayView) ElectricLime else Color.DarkGray
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(if (isAllDayView) stringResource(R.string.kitchen_view_all_day) else stringResource(R.string.kitchen_view_orders), color = if (isAllDayView) Color.Black else Color.White, fontSize = 12.sp)
                                    }
                                }
                                
                                Button(
                                    onClick = onExitKitchenMode,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(stringResource(R.string.action_exit), color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } 
        
                        if (isAllDayView) {
                            // --- ALL DAY VIEW LOGIC ---
                            val aggregatedItems = uiState.activeOrders
                                .filter { it.fulfillmentStatus != "READY" && it.fulfillmentStatus != "COMPLETED" }
                                .flatMap { it.items }
                                .filter { !it.isCompleted }
                                .groupBy { it.name }
                                .mapValues { (_, items) -> items.sumOf { it.quantity } }
                                .toList()
                                .sortedByDescending { it.second }

                            if (aggregatedItems.isEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.kitchen_empty_active), color = SecondaryText, fontSize = 20.sp)
                                    }
                                }
                            } else {
                                items(aggregatedItems) { (name, totalQty) ->
                                    AllDaySummaryCard(name, totalQty)
                                }
                            }
                        } else {
                            // --- ORDER VIEW LOGIC ---
                            if (uiState.activeOrders.isEmpty()) {
                               item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.kitchen_empty_quiet), color = SecondaryText, fontSize = 20.sp)
                                    }
                               }
                            } else {
                                items(uiState.activeOrders) { transaction ->
                                    OrderCard(
                                        transaction = transaction, 
                                        isSimplifiedFlow = uiState.isSimplifiedFlow,
                                        onBump = { viewModel.bumpOrder(transaction) },
                                        onComplete = { viewModel.completeOrder(transaction) },
                                        onItemClick = { index -> viewModel.toggleItemCompletion(transaction, index) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllDaySummaryCard(name: String, quantity: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = CurbOSShapes.medium,
        border = BorderStroke(1.dp, SecondaryText),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quantity.toString(),
                color = ElectricLime,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = name.uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


@Composable
fun OrderCard(
    transaction: Transaction, 
    isSimplifiedFlow: Boolean, 
    onBump: () -> Unit, 
    onComplete: () -> Unit,
    onItemClick: (Int) -> Unit
) {
    val elapsedTime = System.currentTimeMillis() - transaction.timestamp
    val elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime)
    
    val accentColor = when {
        elapsedMinutes < 5 -> ElectricLime
        elapsedMinutes < 10 -> SafetyOrange
        else -> Color.Red
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = CurbOSShapes.large,
        border = BorderStroke(2.dp, accentColor.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .shadow(
                elevation = if (transaction.fulfillmentStatus == "READY") 20.dp else 4.dp, 
                shape = CurbOSShapes.large,
                spotColor = accentColor.copy(alpha = 0.2f)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Huge Order Number and Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${transaction.orderNumber ?: "???"}",
                        color = accentColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        lineHeight = 32.sp
                    )
                    if (!transaction.customerName.isNullOrBlank()) {
                        Text(
                            text = transaction.customerName.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CurbOSShapes.medium,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${elapsedMinutes} MIN",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            // Items List: High Contrast and Large
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                transaction.items.forEachIndexed { index, itemRow ->
                    val isCompleted = itemRow.isCompleted
                    val opacity = if (isCompleted) 0.3f else 1.0f
                    val textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null

                    Column(modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable { onItemClick(index) }
                    ) {
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.alpha(opacity)) {
                            // Quantity Badge
                            Surface(
                                color = if (isCompleted) Color.Gray else Color.White,
                                shape = CurbOSShapes.small,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = itemRow.quantity.toString(),
                                        color = Color.Black,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        textDecoration = textDecoration
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = itemRow.name.uppercase(),
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 22.sp,
                                textDecoration = textDecoration
                            )
                        }
                        
                        // Modifiers
                        if (itemRow.modifiers.isNotEmpty()) {
                            Text(
                                text = itemRow.modifiers.joinToString(", ") { it.uppercase() },
                                color = ElectricLime.copy(alpha = 0.8f * opacity),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 44.dp, top = 4.dp),
                                textDecoration = textDecoration
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Button (The "Bump")
            Button(
                onClick = onBump,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("bump_button_${transaction.orderNumber}"), 
                shape = CurbOSShapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when(transaction.fulfillmentStatus) {
                        "PENDING" -> accentColor
                        "IN_PROGRESS" -> SafetyOrange
                        "READY" -> Color.Red
                        else -> accentColor
                    },
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = when(transaction.fulfillmentStatus) {
                        "PENDING" -> if (isSimplifiedFlow) stringResource(R.string.status_ready) else stringResource(R.string.status_cook)
                        "IN_PROGRESS" -> stringResource(R.string.status_ready)
                        "READY" -> stringResource(R.string.status_deliver)
                        else -> stringResource(R.string.status_complete)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Optional direct completion button if not already in the last stage
            if (transaction.fulfillmentStatus != "READY") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onComplete,
                    border = BorderStroke(1.dp, SecondaryText),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(stringResource(R.string.action_fast_ready), color = SecondaryText)
                }
            }
            
            // Small Info Tag
            Text(
                text = stringResource(R.string.label_status, transaction.fulfillmentStatus),
                color = SecondaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
            )
        }
    }
}
