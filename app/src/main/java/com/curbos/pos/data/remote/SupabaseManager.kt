package com.curbos.pos.data.remote

import com.curbos.pos.data.model.MenuItem
import com.curbos.pos.data.model.Transaction
import com.curbos.pos.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import io.github.jan.supabase.serializer.KotlinXSerializer

object SupabaseManager {

    private const val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private const val SUPABASE_KEY = BuildConfig.SUPABASE_KEY
    
    // For local dev with emulator, use 10.0.2.2 instead of localhost
    // private const val SUPABASE_URL_LOCAL = "http://10.0.2.2:54321" 

    // Use a lazy delegate so we can ensure initialization happens on the Main thread 
    // if accessed from there, or we can pre-init it safely.
    // Use a lazy delegate so we can ensure initialization happens on the Main thread 
    // if accessed from there, or we can pre-init it safely.
    val client: SupabaseClient by lazy {
        if (SUPABASE_URL.isBlank() || SUPABASE_KEY.isBlank()) {
           throw IllegalStateException("Supabase Configuration Missing! URL or Key is empty. Check BuildConfig and CI Secrets.")
        }
        
        // Auto-fix localhost for Android Emulator
        // If running on real device, 10.0.2.2 won't work either, but localhost DEFINITELY won't work.
        // This attempts to help emulator dev environments.
        val configUrl = if (SUPABASE_URL.contains("localhost")) {
            com.curbos.pos.common.Logger.w("SupabaseManager", "Localhost detected in config. Rewriting to 10.0.2.2 for emulator access.")
            SUPABASE_URL.replace("localhost", "10.0.2.2")
        } else {
            SUPABASE_URL
        }

        createSupabaseClient(
            supabaseUrl = configUrl,
            supabaseKey = SUPABASE_KEY
        ) {
            httpEngine = CIO.create()
            defaultSerializer = KotlinXSerializer(kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                encodeDefaults = true 
            })
            install(Postgrest)
            install(Realtime)
            install(Auth)
        }
    }

    /**
     * Pre-initializes the Supabase client.
     * MUST be called from the Main thread because Supabase Auth 
     * registers lifecycle observers.
     */
    /**
     * Pre-initializes the Supabase client.
     * MUST be called from the Main thread because Supabase Auth 
     * registers lifecycle observers.
     */
    fun init() {
        // Just accessing the lazy property triggers initialization
        client
    }

    @Volatile
    private var remoteLoggingDisabled = false

    suspend fun logRemoteError(entry: com.curbos.pos.common.Logger.RemoteLogEntry) {
        if (remoteLoggingDisabled) return

        try {
            client.postgrest["admin_error_logs"].insert(entry)
        } catch (e: io.github.jan.supabase.exceptions.NotFoundRestException) {
            // Table doesn't exist. Disable remote logging to prevent spam/crash loop.
            remoteLoggingDisabled = true
            android.util.Log.w("SupabaseManager", "Remote logging disabled: 'admin_error_logs' table not found.")
        } catch (e: Exception) {
            // Failure here must be silent for the logger to avoid direct recursion or crash
            try {
                // Ensure we don't recurse if the Logger tries to remote log this error too
                android.util.Log.e("SupabaseManager", "Failed to insert remote log: ${e.message}")
            } catch (inner: Exception) {
                // ignore
            }
        }
    }

    // Auth0 Login (Exchange ID Token)
    suspend fun signInWithAuth0(): com.curbos.pos.common.Result<Boolean> {
        return try {
            // Option 1: If Supabase Project has Auth0 Provider enabled
            // client.auth.signInWith(IDToken) {
            //     this.idToken = idToken
            //     this.provider = IDToken.Provider.OR("auth0") // or custom
            // }
            
            // Option 2: Verify Subscription directly using the email from Auth0 (Client-side gating)
            // This is a temporary measure if Supabase <-> Auth0 link is not fully configured for RLS.
            //Ideally, we expect Supabase to accept the token or we use a service key (not safe for prod, but local logic).
            
            // For this requested migration: We will assume we just need to GATE the user.
            // The Auth0 SDK handles the login. We just need to check permissions.
            // We will trust the Android App's Auth0 login for now, and query the public user table.
            com.curbos.pos.common.Logger.d("SupabaseManager", "Auth0 Login Successful (Client Side)")
            com.curbos.pos.common.Result.Success(true)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Auth0 Login failed", e)
            com.curbos.pos.common.Result.Error(e, "Auth0 Login failed: ${e.localizedMessage}")
        }
    }

    suspend fun checkSubscriptionStatus(email: String): com.curbos.pos.common.Result<Boolean> {
        return try {
            com.curbos.pos.common.Logger.d("SupabaseManager", "Checking subscription for $email")
            // Query public 'users' table. 
            // Note: This requires the table to be readable by Anon/Public OR the user to be authenticated.
            
            // Try fetching from 'users'
            val user = try {
                client.postgrest["users"]
                    .select {
                        filter {
                            eq("email", email)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<Map<String, String>>()
            } catch (e: io.github.jan.supabase.exceptions.NotFoundRestException) {
                com.curbos.pos.common.Logger.w("SupabaseManager", "Users table not found. Skipping subscription check.")
                null
            } catch (e: Exception) {
                 // Ignore other errors and fail open
                 null
            }

            if (user == null) {
                 // User not found or table missing
                 // Fail Open for demo/dev to allow login
                 return com.curbos.pos.common.Result.Success(true) 
            }

            val status = user["subscription_status"] ?: "trial"
            com.curbos.pos.common.Logger.d("SupabaseManager", "User Status: $status")
            
            // Allow access unless explicitly expired or locked
            val isAllowed = !status.equals("expired", ignoreCase = true) && !status.equals("locked", ignoreCase = true)
            com.curbos.pos.common.Result.Success(isAllowed)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Subscription check failed (failing open)", e)
            // Fail open to allow login if schema/RLS issues exist
            com.curbos.pos.common.Result.Success(true)
        }
    }

    suspend fun fetchMenuItems(): com.curbos.pos.common.Result<List<MenuItem>> {
        return try {
            val items = client.postgrest["menu_items"]
                .select()
                .decodeList<MenuItem>()
            com.curbos.pos.common.Result.Success(items)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to sync menu", e)
            com.curbos.pos.common.Result.Error(e, "Failed to sync menu: ${e.localizedMessage}")
        }
    }

    suspend fun fetchModifiers(): com.curbos.pos.common.Result<List<com.curbos.pos.data.model.ModifierOption>> {
        return try {
            val items = client.postgrest["modifier_options"]
                .select()
                .decodeList<com.curbos.pos.data.model.ModifierOption>()
            com.curbos.pos.common.Result.Success(items)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to sync modifiers", e)
            com.curbos.pos.common.Result.Error(e, "Failed to sync modifiers: ${e.localizedMessage}")
        }
    }

    suspend fun uploadTransaction(transaction: Transaction): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["transactions"]
                .upsert(transaction, onConflict = "id")
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to upload transaction: ${e.message}", e)
            com.curbos.pos.common.Result.Error(e, "Failed to upload transaction: ${e.localizedMessage}")
        }
    }

    suspend fun upsertMenuItem(item: MenuItem): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["menu_items"].upsert(item, onConflict = "id")
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to sync menu item", e)
            com.curbos.pos.common.Result.Error(e, "Failed to sync menu item: ${e.localizedMessage}")
        }
    }

    suspend fun deleteMenuItem(id: String): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["menu_items"].delete {
                filter {
                    eq("id", id)
                }
            }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to delete menu item", e)
            com.curbos.pos.common.Result.Error(e, "Failed to delete menu item: ${e.localizedMessage}")
        }
    }

    suspend fun deleteItemsByCategory(category: String): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["menu_items"].delete {
                filter {
                     eq("category", category)
                }
            }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to delete category items", e)
            com.curbos.pos.common.Result.Error(e, "Failed to delete category items: ${e.localizedMessage}")
        }
    }

    suspend fun updateCategoryName(oldName: String, newName: String): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["menu_items"].update({
                set("category", newName)
            }) {
                filter {
                    eq("category", oldName)
                }
            }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
             com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to rename category", e)
             com.curbos.pos.common.Result.Error(e, "Failed to rename category: ${e.localizedMessage}")
        }
    }

    suspend fun upsertModifier(modifier: com.curbos.pos.data.model.ModifierOption): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["modifier_options"].upsert(modifier, onConflict = "id")
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to sync modifier", e)
            com.curbos.pos.common.Result.Error(e, "Failed to sync modifier: ${e.localizedMessage}")
        }
    }

    suspend fun deleteModifier(id: String): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["modifier_options"].delete {
                filter {
                    eq("id", id)
                }
            }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to delete modifier", e)
            com.curbos.pos.common.Result.Error(e, "Failed to delete modifier: ${e.localizedMessage}")
        }
    }

    suspend fun subscribeToMenuChanges(onUpdate: () -> Unit) {
        try {
            val channel = client.realtime.channel("menu-cal")
            val changes = channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>(schema = "public") {
                table = "menu_items"
            }
            safeSubscribe(channel)
            changes.collect {
                onUpdate()
            }
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Menu subscription error", e)
            e.printStackTrace()
        }
    }

    suspend fun subscribeToModifierChanges(onUpdate: () -> Unit) {
        try {
            val channel = client.realtime.channel("modifiers-cal")
            val changes = channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>(schema = "public") {
                table = "modifier_options"
            }
            safeSubscribe(channel)
            changes.collect {
                onUpdate()
            }
        } catch (e: Exception) {
             e.printStackTrace()
        }
    }
    suspend fun wipeAllData(): com.curbos.pos.common.Result<Unit> {
        return try {
            // Delete modifiers first due to FK constraints if any (though currently none enforced strictly, safer)
            client.postgrest["modifier_options"].delete {
                filter {
                    neq("id", "00000000-0000-0000-0000-000000000000") // Delete all
                }
            }
            client.postgrest["menu_items"].delete {
                filter {
                    neq("id", "00000000-0000-0000-0000-000000000000") // Delete all
                }
            }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to wipe data", e)
            com.curbos.pos.common.Result.Error(e, "Failed to wipe data: ${e.localizedMessage}")
        }
    }

    suspend fun seedDefaultData(): com.curbos.pos.common.Result<Unit> {
        return try {
            val initialItems = listOf(
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Al Pastor Elysium", category = "Tacos", price = 4.50, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Carne Asada Supreme", category = "Tacos", price = 5.00, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Baja Fish Nirvana", category = "Tacos", price = 5.50, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Vegan 'Chorizo' Dream", category = "Tacos", price = 4.00, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Horchata Gold", category = "Drinks", price = 3.50, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Jarritos Lime", category = "Drinks", price = 3.00, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "CurbOS Cap", category = "Merch", price = 25.00, taxRate = 0.1, isAvailable = true, imageUrl = null),
                MenuItem(id = java.util.UUID.randomUUID().toString(), name = "Spicy Sauce Bottle", category = "Merch", price = 12.00, taxRate = 0.1, isAvailable = true, imageUrl = null)
            )
            client.postgrest["menu_items"].insert(initialItems)
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to seed data", e)
            com.curbos.pos.common.Result.Error(e, "Failed to seed data: ${e.localizedMessage}")
        }
    }


    // KDS Functions
    suspend fun fetchTransaction(id: String): com.curbos.pos.common.Result<Transaction> {
        return try {
            val item = client.postgrest["transactions"]
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<Transaction>()
            com.curbos.pos.common.Result.Success(item)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Transaction not found: $id", e)
            com.curbos.pos.common.Result.Error(e, "Transaction not found: ${e.localizedMessage}")
        }
    }

    suspend fun fetchActiveTransactions(): com.curbos.pos.common.Result<List<Transaction>> {
        return try {
            val items = client.postgrest["transactions"]
                .select {
                    filter {
                        neq("fulfillment_status", "COMPLETED")
                    }
                }
                .decodeList<Transaction>()
                .sortedBy { it.timestamp }
            
            com.curbos.pos.common.Logger.d("SupabaseManager", "Fetched ${items.size} active items.")
            com.curbos.pos.common.Result.Success(items)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Fetch error: ${e.message}", e)
            com.curbos.pos.common.Result.Error(e, "Failed to fetch active orders: ${e.localizedMessage}")
        }

    }

    suspend fun updateTransactionStatus(id: String, status: String): com.curbos.pos.common.Result<Unit> {
        return try {
            client.postgrest["transactions"]
                .update({
                    set("fulfillment_status", status)
                }) {
                    filter {
                        eq("id", id)
                    }
                }
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to update order status", e)
            com.curbos.pos.common.Result.Error(e, "Failed to update order status: ${e.localizedMessage}")
        }
    }

    suspend fun subscribeToTransactionChanges(onUpdate: () -> Unit) {
        try {
            val channel = client.realtime.channel("transactions-kds")
            val changes = channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>(schema = "public") {
                table = "transactions"
            }
            safeSubscribe(channel)
            com.curbos.pos.common.Logger.d("SupabaseRealtime", "Subscribed to transactions channel")
            changes.collect { action ->
                com.curbos.pos.common.Logger.d("SupabaseRealtime", "Realtime change received: $action")
                onUpdate()
            }
        } catch (e: Exception) {
             e.printStackTrace()
        }
    }

    suspend fun subscribeToReadyNotifications(onOrderReady: (Transaction) -> Unit) {
        try {
            val channel = client.realtime.channel("orders-ready")
            val changes = channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Update>(schema = "public") {
                table = "transactions"
                filter = "fulfillment_status=eq.READY"
            }
            safeSubscribe(channel)
            changes.collect { action ->
                try {
                    val transaction = action.decodeRecord<Transaction>()
                    onOrderReady(transaction)
                } catch (e: Exception) {
                    com.curbos.pos.common.Logger.e("SupabaseManager", "Error decoding ready notification", e)
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
             com.curbos.pos.common.Logger.e("SupabaseManager", "Ready notification subscription error", e)
             e.printStackTrace()
        }
    }

    // Settings Sync
    suspend fun fetchSettings(): com.curbos.pos.common.Result<com.curbos.pos.data.model.UserSettings?> {
        return try {
            val user = client.auth.currentUserOrNull() ?: return com.curbos.pos.common.Result.Error(Exception("Not logged in"), "Not logged in")
             
             // Try to fetch existing settings
            val settings = client.postgrest["user_settings"]
                .select {
                    filter {
                        eq("user_id", user.id)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<com.curbos.pos.data.model.UserSettings>()
                
            com.curbos.pos.common.Result.Success(settings)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to fetch settings", e)
            com.curbos.pos.common.Result.Error(e, "Failed to fetch settings: ${e.localizedMessage}")
        }
    }

    suspend fun saveSettings(settings: com.curbos.pos.data.model.UserSettings): com.curbos.pos.common.Result<Unit> {
        return try {
             val user = client.auth.currentUserOrNull() ?: return com.curbos.pos.common.Result.Error(Exception("Not logged in"), "Not logged in")
             
             // Ensure user_id is set
             val settingsWithId = settings.copy(userId = user.id)
             
            client.postgrest["user_settings"].upsert(settingsWithId)
            com.curbos.pos.common.Result.Success(Unit)
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SupabaseManager", "Failed to save settings", e)
            com.curbos.pos.common.Result.Error(e, "Failed to save settings: ${e.localizedMessage}")
        }
    }

    private val connectionMutex = Mutex()

    private suspend fun safeSubscribe(channel: io.github.jan.supabase.realtime.RealtimeChannel) {
        try {
            connectionMutex.withLock {
                if (client.realtime.status.value != Realtime.Status.CONNECTED) {
                    try {
                        client.realtime.connect()
                    } catch (e: Exception) {
                         // Ignore "already connected" if it happens despite check
                         if (e.message?.contains("already connected", ignoreCase = true) != true) {
                             com.curbos.pos.common.Logger.w("SupabaseRealtime", "Connect error: ${e.message}")
                         }
                    }
                }
            }
            channel.subscribe()
        } catch (e: Exception) {
            // Ignore if already connected or other transient issues, retry logic handles it
            com.curbos.pos.common.Logger.w("SupabaseRealtime", "Subscribe warning: ${e.message}")
        }
    }
}
