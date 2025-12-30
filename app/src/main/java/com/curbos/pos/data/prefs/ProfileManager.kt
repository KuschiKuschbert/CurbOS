package com.curbos.pos.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.curbos.pos.common.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "chef_profile_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    init {
        migrateOldPrefs(context)
    }

    private fun migrateOldPrefs(context: Context) {
        val oldPrefs = context.getSharedPreferences("chef_profile", Context.MODE_PRIVATE)
        if (oldPrefs.all.isNotEmpty()) {
            com.curbos.pos.common.Logger.i("ProfileManager", "Migrating old unencrypted preferences to secure storage...")
            val editor = prefs.edit()
            oldPrefs.all.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                }
            }
            editor.apply()
            oldPrefs.edit().clear().apply()
            com.curbos.pos.common.Logger.i("ProfileManager", "Migration complete. Old storage cleared.")
        }
    }

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
    fun saveDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("developer_mode", enabled).apply()
    }

    fun isDeveloperMode(): Boolean {
        return prefs.getBoolean("developer_mode", false)
    }
}
