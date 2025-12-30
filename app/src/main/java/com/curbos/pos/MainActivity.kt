package com.curbos.pos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.curbos.pos.data.local.AppDatabase
import com.curbos.pos.data.CsvExportManager
import com.curbos.pos.ui.screens.AdminScreen
import com.curbos.pos.ui.screens.MenuManagementScreen
import com.curbos.pos.ui.screens.QuickSalesScreen
import com.curbos.pos.ui.screens.SplashScreen
import com.curbos.pos.ui.theme.CurbOSTheme
import kotlinx.coroutines.launch
import com.curbos.pos.data.prefs.ProfileManager
import com.curbos.pos.util.HapticHelper
import com.curbos.pos.util.BiometricHelper
import com.curbos.pos.ui.screens.LoginScreen
import com.curbos.pos.ui.screens.WelcomeScreen
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.curbos.pos.data.worker.SyncWorker
import com.curbos.pos.ui.viewmodel.SalesViewModel

@dagger.hilt.android.AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @javax.inject.Inject lateinit var posDao: com.curbos.pos.data.local.PosDao
    @javax.inject.Inject lateinit var p2pManager: com.curbos.pos.data.p2p.P2PConnectivityManager
    @javax.inject.Inject lateinit var syncManager: com.curbos.pos.data.SyncManager
    // transactionSyncManager removed - replaced by Repository
    @javax.inject.Inject lateinit var profileManager: com.curbos.pos.data.prefs.ProfileManager
    @javax.inject.Inject lateinit var transactionRepository: com.curbos.pos.data.repository.TransactionRepository
    @javax.inject.Inject lateinit var menuRepository: com.curbos.pos.data.repository.MenuRepository
    @javax.inject.Inject lateinit var updateManager: com.curbos.pos.data.UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure Supabase is initialized on the Main thread (required for Auth lifecycle observers)
        com.curbos.pos.data.remote.SupabaseManager.init(this)
        
        // Schedule Periodic Sync (Every 15 minutes)
        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .build()
            
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PeriodicOrderSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )

        setContent {
            CurbOSTheme {
                val context = LocalContext.current
                // ProfileManager is injected
                // HapticHelper and BiometricHelper are View helpers, can stay local or be injected
                val hapticHelper = remember { HapticHelper(context) }
                val biometricHelper = remember { BiometricHelper(this) } 
                val csvExportManager = remember { CsvExportManager(context, posDao) }
                
                // Adaptive UI
                val windowSizeClass = calculateWindowSizeClass(this)

                // Initialize Data Seeder
                LaunchedEffect(Unit) {
                    com.curbos.pos.data.DataSeeder(posDao).seedDataIfEmpty()
                }
                
                // p2pManager is injected
                
                 val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Handle result */ }

                // syncManager and transactionSyncManager are injected

                val hasUpdates by syncManager.hasAvailableUpdates.collectAsState()
                val syncState by syncManager.syncState.collectAsState()
                val scope = rememberCoroutineScope()
                
                val chefName by profileManager.chefNameFlow.collectAsState()
                val isLoggedIn = chefName != null

                // --- NOTIFICATIONS & SYNC ---
                // Only start sync tasks when user is logged in
                LaunchedEffect(isLoggedIn) {
                    if (isLoggedIn) {
                        syncManager.checkForUpdates()
                        // 1. Process Offline Queue
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                            transactionRepository.syncNow()
                        }
    
                        // 2. Listen for READY orders (KDS -> POS)
                        launch {
                            transactionRepository.subscribeToReadyNotifications { transaction ->
                                val name = transaction.customerName ?: "Order #${transaction.orderNumber}"
                                val message = "🔔 ORDER UP! $name is READY!"
                                scope.launch {
                                    com.curbos.pos.common.SnackbarManager.showSuccess(message)
                                }
                            }
                        }
                        
                        // 3. Realtime Menu Sync
                        launch {
                            menuRepository.subscribeToMenuChanges {
                                // Trigger simple refresh or sync
                                scope.launch { syncManager.checkForUpdates() }
                            }
                        }
                        
                        // 4. Periodic Menu Integrity Check (Self-Healing)
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                            syncManager.startMonitoring()
                        }
                        
                        // 5. Restore Settings form Cloud
                        launch(kotlinx.coroutines.Dispatchers.IO) {
                           profileManager.syncCloudSettings()
                        }
                    }
                }
                // Let's stick to Resume/Startup check and the explicit dialog.
                
                if (hasUpdates) {
                    com.curbos.pos.ui.components.SyncConflictDialog(
                        onConfirmSync = {
                            scope.launch { syncManager.syncNow() }
                        },
                        onDismiss = { /* User deferred update */ }
                    )
                }

                // Show Sync Progress/Error
                when (syncState) {
                    is com.curbos.pos.data.SyncState.Syncing -> {
                        // Optional: Show global loading indicator or toast
                         LaunchedEffect(Unit) {
                            com.curbos.pos.common.SnackbarManager.showMessage("Syncing data...")
                        }
                    }
                    is com.curbos.pos.data.SyncState.Success -> {
                        LaunchedEffect(Unit) {
                            com.curbos.pos.common.SnackbarManager.showSuccess("Data synced successfully!")
                        }
                    }
                    is com.curbos.pos.data.SyncState.Error -> {
                        LaunchedEffect(Unit) {
                           com.curbos.pos.common.SnackbarManager.showError("Sync failed: ${(syncState as com.curbos.pos.data.SyncState.Error).message}")
                        }
                    }
                    else -> {}
                }

                // Square Startup Check
                var showSquareInstallDialog by remember { mutableStateOf(false) }
                val currentContext = LocalContext.current
                
                LaunchedEffect(Unit) {
                    val squarePackage = com.curbos.pos.util.SquareHelper.findSquarePackage(currentContext)
                    if (squarePackage == null) {
                        showSquareInstallDialog = true
                    }
                }

                if (showSquareInstallDialog) {
                    AlertDialog(
                        onDismissRequest = { showSquareInstallDialog = false },
                        title = { Text("Square POS Required") },
                        text = { Text("To process card payments, you need to install a Square POS app.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    com.curbos.pos.util.SquareHelper.openPlayStoreForSquare(currentContext)
                                    showSquareInstallDialog = false
                                }
                            ) {
                                Text("Install Now")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSquareInstallDialog = false }) {
                                Text("Later")
                            }
                        }
                    )
                }

                // Check for Updates
                var updateRelease by remember { mutableStateOf<com.curbos.pos.data.remote.GithubRelease?>(null) }
                LaunchedEffect(Unit) {
                    updateRelease = updateManager.checkForUpdate()
                }

                if (updateRelease != null) {
                    AlertDialog(
                        onDismissRequest = { updateRelease = null },
                        title = { Text("Update Available") },
                        text = { 
                            Text("New version ${updateRelease?.tagName} is available.\n\n${updateRelease?.body}") 
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val asset = updateRelease?.assets?.firstOrNull { it.name.endsWith(".apk") }
                                    if (asset != null) {
                                        updateManager.downloadAndInstall(asset.downloadUrl)
                                    }
                                    updateRelease = null
                                }
                            ) {
                                Text("Update Now")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateRelease = null }) {
                                Text("Later")
                            }
                        }
                    )
                }
                
                val navController = rememberNavController()

                // Determine start destination based on profile
                val startDest = if (profileManager.getChefName() == null) "login" else "welcome"
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showNav = currentRoute !in listOf("splash", "login", "welcome")
                val isCompact = windowSizeClass.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact

                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. Navigation Rail (Tablets/Expanded)
                    if (showNav && !isCompact) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            header = {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        ) {
                            Spacer(Modifier.weight(1f))
                            NavigationRailItem(
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sales") },
                                label = { Text("Sales") },
                                selected = currentRoute == "sales",
                                onClick = { 
                                    navController.navigate("sales") { launchSingleTop = true; restoreState = true }
                                }
                            )
                            NavigationRailItem(
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Menu") },
                                label = { Text("Menu") },
                                selected = currentRoute == "menu",
                                onClick = { 
                                    navController.navigate("menu") { launchSingleTop = true; restoreState = true }
                                }
                            )
                            NavigationRailItem(
                                icon = { Icon(Icons.Outlined.SoupKitchen, contentDescription = "Kitchen") },
                                label = { Text("Kitchen") },
                                selected = currentRoute == "kitchen",
                                onClick = { 
                                    navController.navigate("kitchen") { launchSingleTop = true; restoreState = true }
                                }
                            )
                            NavigationRailItem(
                                icon = { Icon(Icons.Filled.Tv, contentDescription = "Display") },
                                label = { Text("Display") },
                                selected = currentRoute == "customer_display",
                                onClick = { 
                                    navController.navigate("customer_display") { launchSingleTop = true; restoreState = true }
                                }
                            )
                            NavigationRailItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Admin") },
                                label = { Text("Admin") },
                                selected = currentRoute == "admin",
                                onClick = { 
                                    biometricHelper.authenticate {
                                        navController.navigate("admin") { launchSingleTop = true; restoreState = true }
                                    }
                                }
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    // Snackbar Handling
                    val snackbarHostState = remember { SnackbarHostState() }
                    LaunchedEffect(Unit) {
                        com.curbos.pos.common.SnackbarManager.messages.collect { message ->
                            // Custom colors/styling could be applied here by checking message.type
                            @Suppress("UNUSED_VARIABLE")
                            val result = snackbarHostState.showSnackbar(
                                message = message.text,
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )
                        }
                    }

                    // 2. Main Content + Bottom Bar (Phones/Compact)
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (showNav && isCompact) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    NavigationBarItem(
                                        modifier = Modifier.testTag("nav_sales"),
                                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sales") },
                                        label = { Text("Sales") },
                                        selected = currentRoute == "sales",
                                        onClick = { 
                                            navController.navigate("sales") {
                                                popUpTo("sales") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        modifier = Modifier.testTag("nav_menu"),
                                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Menu") },
                                        label = { Text("Menu") },
                                        selected = currentRoute == "menu",
                                        onClick = { 
                                            navController.navigate("menu") {
                                                popUpTo("sales") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        modifier = Modifier.testTag("nav_kitchen"),
                                        icon = { Icon(Icons.Outlined.SoupKitchen, contentDescription = "Kitchen") },
                                        label = { Text("Kitchen") },
                                        selected = currentRoute == "kitchen",
                                        onClick = { 
                                            navController.navigate("kitchen") {
                                                popUpTo("sales") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        modifier = Modifier.testTag("nav_display"),
                                        icon = { Icon(Icons.Filled.Tv, contentDescription = "Display") },
                                        label = { Text("Display") },
                                        selected = currentRoute == "customer_display",
                                        onClick = { 
                                            navController.navigate("customer_display") {
                                                popUpTo("sales") { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    NavigationBarItem(
                                        modifier = Modifier.testTag("nav_admin"),
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Admin") },
                                        label = { Text("Admin") },
                                        selected = currentRoute == "admin",
                                        onClick = { 
                                            // Intercept for Biometric Auth
                                            biometricHelper.authenticate {
                                                navController.navigate("admin") {
                                                    popUpTo("sales") { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDest,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it } },
                        exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally { -it } },
                        popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { -it } },
                        popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally { it } }
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onTimeout = {
                                    val next = if (profileManager.getChefName() == null) "login" else "welcome"
                                    navController.navigate(next) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                biometricHelper = biometricHelper,
                                onLoginSuccess = {
                                    navController.navigate("welcome") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("welcome") {
                            WelcomeScreen(
                                profileManager = profileManager,
                                onTimeout = {
                                    navController.navigate("sales") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("sales") {
                            // Use Hilt ViewModel
                            val salesViewModel: SalesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            
                            QuickSalesScreen(
                                viewModel = salesViewModel,
                                hapticHelper = hapticHelper
                            )
                        }
                        composable("menu") {
                            // Collect flow in the composition for the screen
                            val menuItems by posDao.getAllMenuItems().collectAsState(initial = emptyList())
                            val modifiers by posDao.getAllModifiers().collectAsState(initial = emptyList())
                            val coroutineScope = rememberCoroutineScope()
                            
                            MenuManagementScreen(
                                menuItems = menuItems,
                                onSave = { item ->
                                    coroutineScope.launch {
                                        if (menuItems.any { it.id == item.id }) {
                                            posDao.updateMenuItem(item)
                                        } else {
                                            posDao.insertMenuItem(item)
                                        }
                                        // Sync to Cloud
                                        launch(kotlinx.coroutines.Dispatchers.IO) {
                                            menuRepository.upsertMenuItem(item)
                                        }
                                    }
                                },
                                onDelete = { item ->
                                    scope.launch {
                                        posDao.deleteMenuItem(item)
                                        // Sync to Cloud
                                        launch(kotlinx.coroutines.Dispatchers.IO) {
                                            menuRepository.deleteMenuItem(item.id)
                                        }
                                    }
                                },
                                modifiers = modifiers,
                                onSaveModifier = { modifier ->
                                    scope.launch {
                                        if (modifiers.any { it.id == modifier.id }) {
                                            posDao.updateModifier(modifier)
                                        } else {
                                            posDao.insertModifier(modifier)
                                        }
                                        // Sync to Cloud
                                        launch(kotlinx.coroutines.Dispatchers.IO) {
                                            menuRepository.upsertModifier(modifier)
                                        }
                                    }
                                },
                                onDeleteModifier = { modifier ->
                                    scope.launch {
                                        posDao.deleteModifier(modifier)
                                        // Sync to Cloud
                                        launch(kotlinx.coroutines.Dispatchers.IO) {
                                            menuRepository.deleteModifier(modifier.id)
                                        }
                                    }
                                }
                            )
                        }
                        composable("admin") {
                             val adminViewModel: com.curbos.pos.ui.viewmodel.AdminViewModel = androidx.hilt.navigation.compose.hiltViewModel()

                            // Intercepted by BiometricAuth in bottom bar earlier, but double check not strictly needed here
                            AdminScreen(
                                viewModel = adminViewModel,
                                csvExportManager = csvExportManager,
                                onLaunchCustomerDisplay = {
                                    navController.navigate("customer_display") { launchSingleTop = true }
                                },
                                onLaunchP2PSetup = {
                                    navController.navigate("p2p_setup") { launchSingleTop = true }
                                }
                            )
                        }
                        composable("kitchen") {
                            val kitchenViewModel: com.curbos.pos.ui.viewmodel.KitchenViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            com.curbos.pos.ui.screens.KitchenScreen(
                                viewModel = kitchenViewModel,
                                onExitKitchenMode = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                            
                        composable("p2p_setup") {
                             // Request permissions when entering this screen
                             LaunchedEffect(Unit) {
                                  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                      permissionLauncher.launch(arrayOf(
                                          android.Manifest.permission.BLUETOOTH_SCAN,
                                          android.Manifest.permission.BLUETOOTH_ADVERTISE,
                                          android.Manifest.permission.BLUETOOTH_CONNECT,
                                          android.Manifest.permission.ACCESS_FINE_LOCATION,
                                          android.Manifest.permission.ACCESS_WIFI_STATE,
                                          android.Manifest.permission.CHANGE_WIFI_STATE,
                                           android.Manifest.permission.NEARBY_WIFI_DEVICES
                                      ))
                                  } else {
                                      permissionLauncher.launch(arrayOf(
                                          android.Manifest.permission.BLUETOOTH,
                                          android.Manifest.permission.BLUETOOTH_ADMIN,
                                          android.Manifest.permission.ACCESS_FINE_LOCATION,
                                          android.Manifest.permission.ACCESS_WIFI_STATE,
                                          android.Manifest.permission.CHANGE_WIFI_STATE
                                      ))
                                  }
                             }
                             
                             com.curbos.pos.ui.screens.P2PConnectionScreen(
                                 p2pConnectivityManager = p2pManager,
                                 onNavigateBack = { navController.popBackStack() }
                             )
                        }

                        composable("customer_display") {
                             val customerDisplayViewModel: com.curbos.pos.ui.viewmodel.CustomerDisplayViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                             com.curbos.pos.ui.screens.CustomerDisplayScreen(
                                 viewModel = customerDisplayViewModel
                             )
                        }
                    }
                }
            }
            }
        }
    }
}
