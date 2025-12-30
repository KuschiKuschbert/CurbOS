package com.curbos.pos.data

import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.remote.SupabaseManager
import com.curbos.pos.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.ModifierOption
class SyncManager(
    private val posDao: PosDao
) {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _hasAvailableUpdates = MutableStateFlow(false)
    val hasAvailableUpdates = _hasAvailableUpdates.asStateFlow()

    suspend fun checkForUpdates() {
        _syncState.value = SyncState.Checking
        try {
            // Check Menu Items
            val localMenuTime = posDao.getLatestMenuUpdate()
            // We can't efficiently check "max" from remote without a custom RPC or helper, 
            // but we can just fetch the first item ordered by updated_at desc limit 1.
            // For now, let's fetch all metadata or just assume we sync if network is available 
            // for this simpler prototype.
            // BETTER APPROACH for prototype: Fetch ALL and compare max locally (inefficient strictly but fine for small menu).
            
            // Actually, SupabaseManager.fetchMenuItems() gets ALL.
            // Let's optimize: Just fetch headers or small query? No, just fetch all for now.
            // Wait, "Check for updates" implies we don't start the heavy download yet.
            // Let's implement a lighter "head" check if possible, or just download updates.
            
            // For this specific User Request, "automatic synch check... and then choose which data".
            // So we DO need to know IF there is an update before overwriting.
            // Let's fetch the list from remote.
            
            val remoteMenuItemsResult = SupabaseManager.fetchMenuItems()
            if (remoteMenuItemsResult is Result.Success) {
                val remoteItems = remoteMenuItemsResult.data
                val remoteMax = remoteItems.maxOfOrNull { it.updatedAt ?: "" } ?: ""
                val localMax = localMenuTime ?: ""
                
                if (remoteMax > localMax) {
                    _hasAvailableUpdates.value = true
                    _syncState.value = SyncState.UpdateAvailable
                    return
                }
            }

            // Check Modifiers
            val localModTime = posDao.getLatestModifierUpdate()
            val remoteModsResult = SupabaseManager.fetchModifiers()
            if (remoteModsResult is Result.Success) {
                val remoteMods = remoteModsResult.data
                val remoteMax = remoteMods.maxOfOrNull { it.updatedAt ?: "" } ?: ""
                val localMax = localModTime ?: ""

                 if (remoteMax > localMax) {
                    _hasAvailableUpdates.value = true
                    _syncState.value = SyncState.UpdateAvailable
                    return
                }
            }
            
            _syncState.value = SyncState.Idle

        } catch (e: Exception) {
             _syncState.value = SyncState.Error(e.message ?: "Check failed")
        }
    }

    suspend fun syncNow() {
        _syncState.value = SyncState.Syncing
        try {
            // 1. Menu
             when (val result = SupabaseManager.fetchMenuItems()) {
                is Result.Success -> posDao.insertMenuItems(result.data)
                is Result.Error -> throw Exception(result.message)
                else -> {}
            }
            
            // 2. Modifiers
             when (val result = SupabaseManager.fetchModifiers()) {
                is Result.Success -> result.data.forEach { posDao.insertModifier(it) }
                is Result.Error -> throw Exception(result.message)
                else -> {}
            }
            
            _hasAvailableUpdates.value = false
            _syncState.value = SyncState.Success
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }
    suspend fun startRealtimeListening() {
        SupabaseManager.subscribeToMenuChanges {
            // Trigger Sync
             scope.launch {
                syncNow()
            }
        }
        SupabaseManager.subscribeToModifierChanges {
             scope.launch {
                syncNow()
            }
        }
    }
    
    // Self-healing: Periodic check for updates (every 2 minutes)
    suspend fun startMonitoring() {
        while (true) {
            try {
                checkForUpdates()
            } catch (e: Exception) {
               // Ignore errors, retry later
            }
            kotlinx.coroutines.delay(2 * 60 * 1000) // 2 minutes
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Checking : SyncState()
    object UpdateAvailable : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
