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
import com.curbos.pos.data.prefs.ProfileManager

class SyncManager(
    private val posDao: PosDao,
    private val profileManager: ProfileManager
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

    suspend fun performTwoWaySync() {
        _syncState.value = SyncState.Syncing
        try {
            val lastSyncTime = profileManager.getLastMenuSyncTime()
            val newSyncTime = java.time.Instant.now().toString()

            // 1. PUSH: Upload Local Dirty Items
            // Note: We should ideally have 'dirty' flag, but for now we look for items updated after last sync
            // This assumes local clock is roughly in sync, which is risky.
            // A better way is: "Any item where updatedAt > lastSyncTime AND its NOT a fresh download"
            // For this implementation, we will trust the "Convergent Sync" we designed:
            // Sync = Push Local Changes -> Pull Remote Changes.
            
            // Push Menu Items
            val dirtyItems = posDao.getModifiedMenuItems(lastSyncTime)
            if (dirtyItems.isNotEmpty()) {
                SupabaseManager.upsertMenuItems(dirtyItems)
                // Note: If upsert updates timestamp on server, we get it back next sync. That's fine.
            }

            // Push Modifiers
            val dirtyModifiers = posDao.getModifiedModifiers(lastSyncTime)
            if (dirtyModifiers.isNotEmpty()) {
                SupabaseManager.upsertModifiers(dirtyModifiers)
            }

            // 2. PULL: Download Remote Changes (Delta Sync)
            // Fetch items changed since lastSyncTime (or all if never synced)
            // We use a small buffer (e.g. 1 minute) to avoid missing changes due to clock skew
            
            // Menu Items
            when (val result = SupabaseManager.fetchMenuItemsSince(lastSyncTime)) {
                is Result.Success -> {
                    result.data.forEach { item ->
                        if (item.deletedAt != null) {
                            posDao.deleteMenuItem(item) // Hard delete locally for now, or keep as soft delete?
                            // If we keep soft delete, UI must filter. We updated UI to filter.
                            // But keeping dead rows forever in SQLite is bad? 
                            // Actually, soft delete in SQLite allows syncing "Delete" to other devices.
                            // Let's use softDelete in DAO.
                             posDao.softDeleteMenuItem(item.id, item.deletedAt)
                        } else {
                            posDao.insertMenuItem(item)
                        }
                    }
                }
                is Result.Error -> throw Exception(result.message)
                else -> {}
            }

            // Modifiers
            when (val result = SupabaseManager.fetchModifiersSince(lastSyncTime)) {
                is Result.Success -> {
                     result.data.forEach { item ->
                        if (item.deletedAt != null) {
                             posDao.softDeleteModifier(item.id, item.deletedAt)
                        } else {
                            posDao.insertModifier(item)
                        }
                    }
                }
                is Result.Error -> throw Exception(result.message)
                else -> {}
            }

            // 3. SUCCESS: Update Checkpoint
            profileManager.saveLastMenuSyncTime(newSyncTime)
            _hasAvailableUpdates.value = false
            _syncState.value = SyncState.Success

        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SyncManager", "Two-way sync failed", e)
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }
    suspend fun startRealtimeListening() {
        SupabaseManager.subscribeToMenuChanges {
            // Trigger Sync
             scope.launch {
                performTwoWaySync()
            }
        }
        SupabaseManager.subscribeToModifierChanges {
             scope.launch {
                performTwoWaySync()
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
