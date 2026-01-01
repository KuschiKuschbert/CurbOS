package com.curbos.pos.ui.screens

import com.curbos.pos.ui.theme.Taco
import com.curbos.pos.ui.theme.Soda
import com.curbos.pos.ui.theme.Shirt
import com.curbos.pos.ui.theme.IceCreamCone
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.asImageBitmap
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
import com.curbos.pos.data.model.CartItem
import com.curbos.pos.data.model.Customer
import com.curbos.pos.data.model.LoyaltyReward
import com.curbos.pos.ui.theme.ElectricLime
import com.curbos.pos.ui.theme.SafetyOrange
import com.curbos.pos.ui.viewmodel.SalesViewModel
import com.curbos.pos.util.HapticHelper
import com.curbos.pos.util.SquareHelper
import com.curbos.pos.ui.components.PulsatingBackground
import kotlinx.coroutines.launch
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

private fun shareCustomerCard(context: android.content.Context, customer: Customer) {
    val bitmap = com.curbos.pos.util.QRCodeGenerator.generateQRCode(customer.id) ?: return
    
    // Save to cache and share
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = java.io.FileOutputStream(java.io.File(cachePath, "customer_card.png"))
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            java.io.File(cachePath, "customer_card.png")
        )

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_TEXT, "Your ${customer.fullName}'s Taco Passport!")
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Customer Card"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Helper function for Icons
fun getIconForCategory(category: String?): ImageVector {
    val cat = category?.lowercase() ?: ""
    return when {
        cat.contains("taco") -> Icons.Filled.Taco
        cat.contains("mexican") -> Icons.Filled.Taco
        cat.contains("drink") || cat.contains("beverage") -> Icons.Filled.Soda
        cat.contains("merch") || cat.contains("shirt") -> Icons.Filled.Shirt
        cat.contains("dessert") || cat.contains("cream") -> Icons.Filled.IceCreamCone
        else -> Icons.Filled.RestaurantMenu
    }
}

@Composable
fun MenuItemCard(item: MenuItem, onClick: (MenuItem) -> Unit) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f)
    val icon = getIconForCategory(item.category)

    com.curbos.pos.ui.components.CurbOSCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = { onClick(item) }
            ),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        borderColor = if (isPressed) ElectricLime else null
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize() // Padding already inside CurbOSCard
        ) {
            // Subtle Background Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxSize().scale(1.2f).offset(y = 12.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Main Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier.size(32.dp).padding(bottom = 4.dp)
                )
                
                Text(
                    text = item.name, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 2,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${item.price}", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = ElectricLime.copy(alpha = 0.8f)
                )
            }
        }
    }
}





// ...

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSalesScreen(
    viewModel: SalesViewModel,
    hapticHelper: HapticHelper
) {
    val uiState by viewModel.uiState.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState(initial = emptyList())
    val allModifiers by viewModel.modifiers.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // Local State for Modifier Dialog
    var showModifierDialog by remember { mutableStateOf(false) }
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    
    // Recent Orders State
    var showRecentOrders by remember { mutableStateOf(false) }
    
    // UI State
    val categories = remember(menuItems) { menuItems.map { it.category }.distinct() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val filteredItems = remember(selectedCategory, menuItems) {
        if (selectedCategory == null) menuItems else menuItems.filter { it.category == selectedCategory }
    }
    
    // Top Bar Actions
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Sync Indicator (Top Left)
        if (uiState.unsyncedCount > 0) {
            Box(
                modifier = Modifier
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { viewModel.triggerManualSync(context) }
            ) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Default.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${uiState.unsyncedCount} Pending", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        // Recent Orders Button (Top Right)
        IconButton(
            onClick = { showRecentOrders = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.History, contentDescription = "Recent Orders", tint = Color.Gray)
        }
    }

    // Square Integration
    val squareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.processPayment("CARD-SQUARE")
        } else {
            val data = result.data
            val errorCode = data?.getStringExtra("com.squareup.pos.ERROR_CODE")
            val errorDescription = data?.getStringExtra("com.squareup.pos.ERROR_DESCRIPTION")
            
            com.curbos.pos.common.Logger.e("SquareIntegration", "Square Error: ${errorCode} - ${errorDescription}")
            
            if (errorCode != null || errorDescription != null) {
                viewModel.reportError("Payment Failed: ${errorCode} - ${errorDescription}")
            }
        }
    }

    val launchSquarePayment = { amountCents: Int ->
        val validPackage = SquareHelper.findSquarePackage(context)
        if (validPackage != null) {
            val intent = SquareHelper.createChargeIntent(
                amountCents = amountCents,
                note = "CurbOS Order (incl. 2.2% surcharge)",
                metadata = "${uiState.customerName}: " + uiState.cartItems.joinToString { it.menuItem.name },
                squarePackageName = validPackage
            )
            squareLauncher.launch(intent)
        } else {
            viewModel.reportError("Square POS, Restaurants, or Retail app needed!")
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.showScanner()
        } else {
            viewModel.reportError("Camera permission is required to scan cards.")
        }
    }

    val requestCameraAndShowScanner = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
        if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            viewModel.showScanner()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    PulsatingBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 600.dp
            
            // Shared State for Cart Sheet on Phones
            var showCartSheet by remember { mutableStateOf(false) }

            if (isWideScreen) {
                // --- TABLET / WIDE SCREEN LAYOUT (Side-by-Side) ---
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Menu Grid
                    Column(
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(16.dp)
                    ) {
                        MenuContent(
                            menuItems = filteredItems,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            hapticHelper = hapticHelper,
                            onItemSelected = { 
                                selectedMenuItem = it
                                showModifierDialog = true
                            }
                        )
                    }

                    // Right: Cart & Actions
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp), // Inner padding
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                            CartContent(
                                cartItems = uiState.cartItems,
                                totalAmount = uiState.totalAmount,
                                discountAmount = uiState.discountAmount,
                                appliedPromoCode = uiState.appliedPromoCode,
                                customerName = uiState.customerName,
                                selectedCustomer = uiState.selectedCustomer,
                                onCustomerNameChange = { viewModel.updateCustomerName(it) },
                                onRemoveItem = { viewModel.removeFromCart(it) },
                                onApplyPromoCode = { viewModel.applyPromoCode(it) },
                                onAttachCustomerClick = { viewModel.showLoyaltyDialog() },
                                onDetachCustomerClick = { viewModel.detachCustomer() },
                                onScanClick = { requestCameraAndShowScanner() },
                                onPaymentSelected = { type -> 
                                    if (uiState.customerName.isBlank() && uiState.selectedCustomer == null) {
                                        viewModel.reportError("Please enter a Customer Name / Table #")
                                        return@CartContent
                                    }
                                    if (type == "CARD") {
                                        val surcharge = uiState.totalAmount * 0.022
                                        val totalWithSurcharge = uiState.totalAmount + surcharge
                                        val amountCents = (totalWithSurcharge * 100).toInt()
                                        launchSquarePayment(amountCents)
                                    }
                                    else viewModel.processPayment(type)
                                }
                            )
                    }
                }
            } else {
                // --- PHONE / NARROW SCREEN LAYOUT (Stacked) ---
                Box(modifier = Modifier.fillMaxSize()) {
                    // Full Screen Menu
                    Column(modifier = Modifier.padding(16.dp)) {
                         MenuContent(
                            menuItems = filteredItems,
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            hapticHelper = hapticHelper,
                            onItemSelected = { 
                                selectedMenuItem = it
                                showModifierDialog = true
                            }
                        )
                    }

                    // Floating Action Button for Cart
                    if (uiState.cartItems.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showCartSheet = true },
                            containerColor = ElectricLime,
                            contentColor = Color.Black,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .testTag("view_cart_button"),
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                            text = { Text("View Cart ($${"%.2f".format(uiState.totalAmount)})") }
                        )
                    }
                    
                    // Cart Overlay / Dialog
                    if (showCartSheet) {
                        Dialog(onDismissRequest = { showCartSheet = false }) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E1E1E),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.9f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Current Order", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                                        IconButton(onClick = { showCartSheet = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    CartContent(
                                        cartItems = uiState.cartItems,
                                        totalAmount = uiState.totalAmount,
                                        discountAmount = uiState.discountAmount,
                                        appliedPromoCode = uiState.appliedPromoCode,
                                        customerName = uiState.customerName,
                                        selectedCustomer = uiState.selectedCustomer,
                                        onCustomerNameChange = { viewModel.updateCustomerName(it) },
                                        onRemoveItem = { viewModel.removeFromCart(it) },
                                        onApplyPromoCode = { viewModel.applyPromoCode(it) },
                                        onAttachCustomerClick = { viewModel.showLoyaltyDialog() },
                                        onDetachCustomerClick = { viewModel.detachCustomer() },
                                        onScanClick = { requestCameraAndShowScanner() },
                                        onPaymentSelected = { type -> 
                                            if (uiState.customerName.isBlank() && uiState.selectedCustomer == null) {
                                                viewModel.reportError("Please enter a Customer Name / Table #")
                                                return@CartContent
                                            }
                                            showCartSheet = false
                                            if (type == "CARD") {
                                                val validPackage = com.curbos.pos.util.SquareHelper.findSquarePackage(context)
                                                if (validPackage != null) {
                                                    val surcharge = uiState.totalAmount * 0.022
                                                    val totalWithSurcharge = uiState.totalAmount + surcharge
                                                    val amountCents = (totalWithSurcharge * 100).toInt()
                                                    launchSquarePayment(amountCents)
                                                } else {
                                                    viewModel.reportError("Square POS app not found for Card payment")
                                                }
                                            }
                                            else viewModel.processPayment(type)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Payment Dialog
    if (uiState.isPaymentDialogVisible) {
        PaymentSelectionDialog(
            totalAmount = uiState.totalAmount,
            onPaymentSelected = { method ->
                if (uiState.customerName.isBlank()) {
                    viewModel.reportError("Please enter a Customer Name / Table #")
                    return@PaymentSelectionDialog
                }
                if (method == "CARD") {
                    val validPackage = com.curbos.pos.util.SquareHelper.findSquarePackage(context)
                    if (validPackage != null) {
                        val surcharge = uiState.totalAmount * 0.022
                        val totalWithSurcharge = uiState.totalAmount + surcharge
                        val amountCents = (totalWithSurcharge * 100).toInt()
                        launchSquarePayment(amountCents)
                    } else {
                        viewModel.reportError("Square POS app not found for Card payment")
                    }
                } else {
                   viewModel.processPayment(method)
                }
            },
            onDismiss = { viewModel.hidePaymentDialog() }
        )
    }
    
    // Order Success Dialog (Immediate QR)
    if (uiState.lastTransactionId != null) {
        TransactionSuccessDialog(
            transactionId = uiState.lastTransactionId!!,
            webBaseUrl = uiState.webBaseUrl,
            onDismiss = { viewModel.resetTransactionState() }
        )
    }
    
    // Recent Orders Dialog
    // Recent Orders Dialog
    // (State hoisted to top of composable)

    
     if (showRecentOrders) {
          RecentTransactionsDialog(
            transactions = uiState.recentTransactions,
            onTransactionClick = { id ->
                viewModel.reopenTransaction(id)
                showRecentOrders = false
            },
            onDismiss = { showRecentOrders = false }
          )
     }

    // Modifier Dialog
    if (showModifierDialog && selectedMenuItem != null) {
        ModifierSelectionDialog(
            menuItem = selectedMenuItem!!,
            availableModifiers = allModifiers.filter { it.category == null || it.category == selectedMenuItem!!.category },
            onDismiss = { 
                showModifierDialog = false 
                selectedMenuItem = null
            },
            onConfirm = { modifiers ->
                viewModel.addToCart(selectedMenuItem!!, modifiers)
                showModifierDialog = false
                selectedMenuItem = null
            }
        )
    }

    // Loyalty Dialogs
    if (uiState.isLoyaltyDialogVisible) {
        if (uiState.selectedCustomer == null) {
            LoyaltySearchDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.hideLoyaltyDialog() }
            )
        } else {
            LoyaltyRewardsDialog(
                customer = uiState.selectedCustomer!!,
                rewards = uiState.loyaltyRewards,
                onRedeem = { reward -> viewModel.redeemReward(reward) },
                onDismiss = { viewModel.hideLoyaltyDialog() }
            )
        }
    }

    // Scanner Dialog
    if (uiState.isScannerVisible) {
        QRScannerDialog(
            onResult = { result ->
                viewModel.attachCustomerById(result)
                viewModel.hideScanner()
            },
            onDismiss = { viewModel.hideScanner() }
        )
    }
}

// --- NEW COMPONENTS ---

@Composable
fun TransactionSuccessDialog(
    transactionId: String,
    webBaseUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ElectricLime,
                    modifier = Modifier
                        .size(64.dp)
                        .background(ElectricLime.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape)
                        .padding(16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Order Placed!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricLime
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Scan to track status",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // QR Code Generation
                val qrBitmap = remember(transactionId) {
                    generateQrCode("$webBaseUrl/curbos/order/$transactionId")
                }
                
                if (qrBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = qrBitmap,
                        contentDescription = "Order QR Code",
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Done", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsDialog(
    transactions: List<com.curbos.pos.data.model.Transaction>,
    onTransactionClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
     Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Orders", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No recent orders found", color = Color.Gray)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
     }
}

@Composable
fun RecentTransactionItem(
    transaction: com.curbos.pos.data.model.Transaction,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${transaction.orderNumber} - ${transaction.customerName ?: "Guest"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${transaction.paymentMethod} • $${"%.2f".format(transaction.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.History, contentDescription = null, tint = ElectricLime.copy(alpha = 0.5f))
        }
    }
}

fun generateQrCode(content: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// --- SUB-COMPONENTS TO CLEAN UP LAYOUT ---

@Composable
fun MenuContent(
    menuItems: List<MenuItem>, 
    categories: List<String?>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    hapticHelper: HapticHelper,
    onItemSelected: (MenuItem) -> Unit
) {
    Text(
        "Menu", 
        style = MaterialTheme.typography.headlineMedium, 
        color = ElectricLime,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    // Category Filter
    ScrollableTabRow(
        selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = ElectricLime,
        indicator = { tabPositions ->
            if (selectedCategory == null) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[0]),
                    color = ElectricLime
                )
            } else {
                 val index = categories.indexOf(selectedCategory) + 1
                 if (index < tabPositions.size) {
                     TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[index]),
                        color = ElectricLime
                    )
                 }
            }
        }
    ) {
        Tab(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            text = { Text("All") }
        )
        categories.forEach { category ->
            if (category != null) {
                Tab(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    text = { Text(category) }
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))

    // Grid uses pre-filtered items passed in as 'menuItems'
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("menu_grid")
    ) {
        items(menuItems) { item ->
            MenuItemCard(item = item) { clickedItem ->
                hapticHelper.vibrateClick()
                onItemSelected(clickedItem)
            }
        }
    }

    
    // CurbOS Branding (Bottom of Menu Grid)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "CURBOS",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.alpha(0.3f)
        )
    }
}

@Composable
fun CartContent(
    cartItems: List<CartItem>,
    totalAmount: Double,
    discountAmount: Double,
    appliedPromoCode: String?,
    customerName: String,
    selectedCustomer: Customer?,
    onCustomerNameChange: (String) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onApplyPromoCode: (String) -> Unit,
    onAttachCustomerClick: () -> Unit,
    onDetachCustomerClick: () -> Unit,
    onScanClick: () -> Unit,
    onPaymentSelected: (String) -> Unit
) {
     val surcharge = totalAmount * 0.022
     val cardTotal = totalAmount + surcharge
     var promoCodeInput by remember { mutableStateOf("") }
     
     // Update input if code is applied externally or cleared
     LaunchedEffect(appliedPromoCode) {
         if (appliedPromoCode == null) promoCodeInput = ""
     }

     Column(modifier = Modifier.fillMaxSize()) {
        // --- CUSTOMER / LOYALTY BAR ---
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            if (selectedCustomer != null) {
                // Customer Attached View
                Card(
                    colors = CardDefaults.cardColors(containerColor = ElectricLime.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onAttachCustomerClick() } // Click to open rewards
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                selectedCustomer.fullName ?: selectedCustomer.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${selectedCustomer.currentRank} • ${selectedCustomer.redeemableMiles.toInt()} Miles",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = onDetachCustomerClick) {
                            Icon(Icons.Default.Close, contentDescription = "Detach", tint = Color.Gray)
                        }
                    }
                }
            } else {
                // Attach Button
                Button(
                    onClick = onAttachCustomerClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Filled.PersonAdd, 
                        contentDescription = null, 
                        tint = ElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach Customer / Loyalty", color = ElectricLime)
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = { onScanClick() },
                    modifier = Modifier.weight(0.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, ElectricLime),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner, 
                        contentDescription = null, 
                        tint = ElectricLime,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Cart List
        LazyColumn(
            modifier = Modifier.weight(1f).testTag("cart_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cartItems) { cartItem ->
                CartItemRow(
                    cartItem = cartItem, 
                    onRemove = { onRemoveItem(cartItem) } 
                )
                HorizontalDivider(color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Totals Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyLarge, color = Color.White)
                // If discount exists, subtotal is Total + Discount
                val subtotalDisplay = if (discountAmount > 0) totalAmount + discountAmount else totalAmount
                Text("$%.2f".format(subtotalDisplay), style = MaterialTheme.typography.bodyLarge, color = Color.White)
            }
            
            if (discountAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Discount (${appliedPromoCode ?: ""}):", style = MaterialTheme.typography.bodyLarge, color = ElectricLime)
                    Text("-$%.2f".format(discountAmount), style = MaterialTheme.typography.bodyLarge, color = ElectricLime)
                }
            } else {
                 Spacer(modifier = Modifier.height(8.dp))
                 // Promo Code Input
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it },
                        placeholder = { Text("Promo Code", color = Color.Gray) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ElectricLime,
                            focusedBorderColor = ElectricLime,
                            unfocusedBorderColor = Color.Gray
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onApplyPromoCode(promoCodeInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                        modifier = Modifier.height(50.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Apply", color = Color.Black)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Customer Name
            OutlinedTextField(
                value = customerName,
                onValueChange = { onCustomerNameChange(it) },
                label = { Text("Customer Name / Table #", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ElectricLime,
                    focusedBorderColor = ElectricLime,
                    unfocusedBorderColor = Color.Gray
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Card Surcharge (1.6%):", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Text("$%.2f".format(surcharge), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Card Total:", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("$%.2f".format(cardTotal), style = MaterialTheme.typography.titleLarge, color = ElectricLime)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Button(
                onClick = { onPaymentSelected("CARD") },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null) 
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("PAY CARD ($%.2f)".format(cardTotal), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onPaymentSelected("CASH") },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("pay_cash_button")
            ) {
                Text("PAY CASH ($%.2f)".format(totalAmount), style = MaterialTheme.typography.titleMedium, color = Color.Black)
            }
        }
    }
}



@Composable
fun CartItemRow(cartItem: CartItem, onRemove: (CartItem) -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = cartItem.menuItem.name, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                if (cartItem.modifiers.isNotEmpty()) {
                    Text(
                        text = cartItem.modifiers.joinToString { 
                            val sign = if (it.priceDelta >= 0) "+" else ""
                            "${it.name} ($sign${it.priceDelta})" 
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$${String.format("%.2f", cartItem.totalPrice)}", color = ElectricLime)
            IconButton(onClick = { onRemove(cartItem) }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
            }
        }
    }
}

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
            shape = MaterialTheme.shapes.medium,
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
                    Text("Card Surcharge (1.6%):", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                    Text("$%.2f".format(surcharge), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray)
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
                    modifier = buttonModifier
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null) 
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("PAY CARD ($%.2f)".format(cardTotal), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPaymentSelected("CASH") },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLime),
                    modifier = buttonModifier
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PAY CASH ($%.2f)".format(totalAmount), style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun LoyaltySearchDialog(
    viewModel: SalesViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var phoneInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CurbOS Loyalty", style = MaterialTheme.typography.headlineMedium, color = ElectricLime)
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = ElectricLime,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricLime
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Search Phone", modifier = Modifier.padding(16.dp), color = if (selectedTab == 0) ElectricLime else Color.Gray)
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("All Customers", modifier = Modifier.padding(16.dp), color = if (selectedTab == 1) ElectricLime else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // --- PHONE SEARCH TAB ---
                    Column {
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Phone Number") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = ElectricLime,
                                focusedBorderColor = ElectricLime,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = ElectricLime,
                                unfocusedLabelColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { viewModel.searchCustomer(phoneInput); onDismiss() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLime)
                        ) {
                            Text("Search / Attach", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // --- ALL CUSTOMERS / DIRECTORY TAB ---
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    viewModel.searchAllCustomers(it)
                                },
                                label = { Text("Search by Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = ElectricLime,
                                    focusedBorderColor = ElectricLime,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.syncAllCustomers() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync All", tint = ElectricLime)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.allCustomers) { customer ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.attachCustomer(customer)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(customer.fullName ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(customer.phoneNumber ?: "No Phone", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${customer.redeemableMiles.toInt()} Miles", color = ElectricLime, fontWeight = FontWeight.Bold)
                                            Text(customer.currentRank, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
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
fun LoyaltyRewardsDialog(
    customer: Customer,
    rewards: List<LoyaltyReward>,
    onRedeem: (LoyaltyReward) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Rewards", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.Gray) }
                }
                
                // Header
                Card(
                     colors = CardDefaults.cardColors(containerColor = ElectricLime.copy(alpha = 0.1f)),
                     modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Balance: ${customer.redeemableMiles.toInt()} Miles", color = ElectricLime, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("Rank: ${customer.currentRank}", color = Color.White)
                            }
                            Button(
                                onClick = { 
                                    shareCustomerCard(context, customer)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, ElectricLime)
                            ) {
                                Icon(Icons.Default.Share, null, tint = ElectricLime, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Card", color = ElectricLime)
                            }
                        }
                    }
                }
                
                Text("Available Rewards", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rewards.sortedBy { it.costMiles }) { reward ->
                        val canAfford = customer.redeemableMiles >= reward.costMiles
                        RewardItemRow(reward = reward, canAfford = canAfford, onRedeem = { onRedeem(reward) })
                    }
                }
            }
        }
    }
}

@Composable
fun QRScannerDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as androidx.lifecycle.LifecycleOwner
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                onResult(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    selector,
                                    preview,
                                    imageAnalysis
                                )
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(200.dp).Border(2.dp, ElectricLime, RoundedCornerShape(8.dp)))
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun RewardItemRow(reward: LoyaltyReward, canAfford: Boolean, onRedeem: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canAfford) 1f else 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(reward.description, color = Color.White, fontWeight = FontWeight.Bold)
                Text("${reward.costMiles} Miles", color = ElectricLime)
            }
            Button(
                onClick = onRedeem,
                enabled = canAfford,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricLime,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text("Redeem", color = if (canAfford) Color.Black else Color.Gray)
            }
        }
    }
}
