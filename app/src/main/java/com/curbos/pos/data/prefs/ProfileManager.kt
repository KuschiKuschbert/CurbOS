package com.curbos.pos.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileManager(context: Context) {
    private val prefs = context.getSharedPreferences("chef_profile", Context.MODE_PRIVATE)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    private val _chefNameFlow = MutableStateFlow(getChefName())
    val chefNameFlow: StateFlow<String?> = _chefNameFlow.asStateFlow()

    fun saveChefName(name: String) {
        prefs.edit().putString("chef_name", name).apply()
        _chefNameFlow.value = name
    }

    fun getChefName(): String? {
        return prefs.getString("chef_name", null)
    }

    fun clearProfile() {
        prefs.edit().clear().apply()
        _chefNameFlow.value = null
    }

    fun saveSimplifiedKitchenFlow(enabled: Boolean) {
        prefs.edit().putBoolean("simplified_kds", enabled).apply()
        // Sync to Cloud
        scope.launch {
            val chef = getChefName()
            val settings = com.curbos.pos.data.model.UserSettings(
                chefName = chef, // Best effort
                simplifiedKds = enabled
            )
            com.curbos.pos.data.remote.SupabaseManager.saveSettings(settings)
        }
    }

    fun isSimplifiedKitchenFlow(): Boolean {
        return prefs.getBoolean("simplified_kds", false)
    }

    fun saveWebBaseUrl(url: String) {
        val sanitized = url.trim().lowercase().removeSuffix("/")
        prefs.edit().putString("web_base_url", sanitized).apply()
    }

    fun getWebBaseUrl(): String {
        return prefs.getString("web_base_url", "https://prepflow.org") ?: "https://prepflow.org"
    }
    
    suspend fun syncCloudSettings() {
        val result = com.curbos.pos.data.remote.SupabaseManager.fetchSettings()
        if (result is com.curbos.pos.common.Result.Success) {
            val settings = result.data
            if (settings != null) {
                // Restore settings locally
                if (settings.simplifiedKds != isSimplifiedKitchenFlow()) {
                    prefs.edit().putBoolean("simplified_kds", settings.simplifiedKds).apply()
                }
                if (settings.chefName != null && settings.chefName != getChefName()) {
                     saveChefName(settings.chefName)
                }
                com.curbos.pos.common.Logger.d("ProfileManager", "Settings synced from cloud: $settings")
            }
        }
    }
}
