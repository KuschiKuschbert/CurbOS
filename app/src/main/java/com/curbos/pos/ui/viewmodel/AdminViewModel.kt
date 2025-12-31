package com.curbos.pos.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.curbos.pos.common.BaseViewModel
import com.curbos.pos.common.Result
import com.curbos.pos.common.SnackbarManager
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.model.Transaction

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AdminUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val totalRevenue: Double = 0.0,
    val totalTx: Int = 0,
    val isSimplifiedKds: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val latestRelease: com.curbos.pos.data.remote.GithubRelease? = null,
    val webBaseUrl: String = "https://prepflow.org",
    val isDeveloperMode: Boolean = false,
    val downloadProgress: Int = 0
)

@dagger.hilt.android.lifecycle.HiltViewModel
class AdminViewModel @javax.inject.Inject constructor(
    private val posDao: PosDao,
    private val profileManager: com.curbos.pos.data.prefs.ProfileManager,
    private val transactionRepository: com.curbos.pos.data.repository.TransactionRepository,
    private val menuRepository: com.curbos.pos.data.repository.MenuRepository,
    private val updateManager: com.curbos.pos.data.UpdateManager
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadDailyStats()
        loadSettings()
        
        viewModelScope.launch {
            updateManager.downloadProgress.collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
            }
        }
    }
    
    // ... (existing functions)

    fun forceSyncOrders() {
        _uiState.update { it.copy(isLoading = true) }
        launchCatching {
            withContext(Dispatchers.IO) {
                transactionRepository.syncNow()
            }
            // Add a small delay for UX so the loading spinner shows for a bit
            kotlinx.coroutines.delay(1000)
             _uiState.update { it.copy(isLoading = false) }
             SnackbarManager.showSuccess("Manual Sync Triggered")
        }
    }
    
    fun loadSettings() {
        _uiState.update { 
            it.copy(
                isSimplifiedKds = profileManager.isSimplifiedKitchenFlow(),
                webBaseUrl = profileManager.getWebBaseUrl(),
                isDeveloperMode = profileManager.isDeveloperMode()
            ) 
        }
    }

    fun updateWebBaseUrl(url: String) {
        profileManager.saveWebBaseUrl(url)
        _uiState.update { it.copy(webBaseUrl = profileManager.getWebBaseUrl()) }
    }
    
    fun toggleSimplifiedKds(enabled: Boolean) {
        profileManager.saveSimplifiedKitchenFlow(enabled)
        _uiState.update { it.copy(isSimplifiedKds = enabled) }
        launchCatching {
            SnackbarManager.showMessage(if (enabled) "Simplified Flow Enabled (Pending -> Ready)" else "Standard Flow Enabled (Pending -> Cook -> Ready)")
        }
    }

    fun loadDailyStats() {
        _uiState.update { it.copy(isLoading = true) }
        launchCatching {
            withContext(Dispatchers.IO) {
                // Determine start/end of day
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endOfDay = calendar.timeInMillis
                
                val transactions = posDao.getTransactionsForDay(startOfDay, endOfDay)
                
                _uiState.update { 
                    it.copy(
                        transactions = transactions,
                        totalRevenue = transactions.sumOf { tx -> tx.totalAmount },
                        totalTx = transactions.size,
                        isLoading = false
                    ) 
                }
            }
        }
    }

    fun resetToDemoData() {
        _uiState.update { it.copy(isLoading = true) }
        launchCatching {
            // 1. Wipe
            menuRepository.wipeAllData()
            
            // 2. Seed
            when (val result = menuRepository.seedDefaultData()) {
                is Result.Success -> {
                    SnackbarManager.showSuccess("Demo data restored!")
                    syncMenu() // Refresh local DB
                }
                is Result.Error -> {
                    SnackbarManager.showError("Failed to seed data: ${result.message}")
                }
                else -> {}
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearAllData() {
        _uiState.update { it.copy(isLoading = true) }
        launchCatching {
            when (val result = menuRepository.wipeAllData()) {
                is Result.Success -> {
                    SnackbarManager.showSuccess("All data cleared from Cloud!")
                    // Local wipe via Sync or manual?
                    posDao.clearMenuItems()
                    // Modifiers clear?
                    // Ideally we syncMenu() and getting empty list clears local. 
                    // But Supabase return empty might need to be handled by delete logic in Sync.
                    // For now, let's just trigger syncMenu() which might not delete items if result is empty list (depends on Sync logic).
                    // Actually, fetchMenuItems returns list. If list is empty, insertions won't remove old ones unless we explicitly clear local.
                    posDao.clearMenuItems()
                    // posDao.clearModifiers() // Need this method in Dao if we want to be thorough
                    
                    SnackbarManager.showMessage("Local data cleared.")
                }
                is Result.Error -> {
                    SnackbarManager.showError("Failed to wipe data: ${result.message}")
                }
                else -> {}
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun syncMenu() {
        _uiState.update { it.copy(isLoading = true) }
        launchCatching {
            // 1. Sync Menu Items
            when (val result = menuRepository.fetchMenuItems()) {
                is Result.Success -> {
                    posDao.insertMenuItems(result.data)
                }
                is Result.Error -> {
                    SnackbarManager.showError("Menu sync failed: ${result.message}")
                }
                Result.Loading -> {}
            }
            
            // 2. Sync Modifiers
            when (val result = menuRepository.fetchModifiers()) {
                is Result.Success -> {
                    result.data.forEach { posDao.insertModifier(it) } // No bulk insert for modifiers yet, loop fine for small sets
                    SnackbarManager.showSuccess("Menu & Modifiers synced!")
                }
                 is Result.Error -> {
                    SnackbarManager.showError("Modifier sync failed: ${result.message}")
                }
                Result.Loading -> {}
            }
            
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        profileManager.saveDeveloperMode(enabled)
        _uiState.update { it.copy(isDeveloperMode = enabled) }
        launchCatching {
             val mode = if (enabled) "Developer Mode (Developer Channel)" else "Standard Mode (Stable Releases)"
             SnackbarManager.showMessage("Switched to $mode")
        }
    }

    fun checkForUpdates() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val isDev = profileManager.isDeveloperMode()
            val release = updateManager.checkForUpdate(isDevMode = isDev)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isUpdateAvailable = release != null,
                    latestRelease = release
                ) 
            }
            if (release == null) {
                SnackbarManager.showMessage("App is up to date")
            }
        }
    }

    fun installUpdate() {
        val asset = uiState.value.latestRelease?.assets?.firstOrNull { it.name.endsWith(".apk") }
        if (asset != null) {
            updateManager.downloadAndInstall(asset.downloadUrl)
        }
    }
}
