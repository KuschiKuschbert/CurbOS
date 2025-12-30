package com.curbos.pos.common

import android.util.Log
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

object Logger {
    private const val GLOBAL_TAG_PREFIX = "CurbOS_"
    private val loggingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Initialize Logger with Timber.
     */
    fun init() {
        if (com.curbos.pos.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // We could plant a SupabaseTree here too, but for now we'll call it explicitly in e()
        // to have more control over what gets sent remotely.
    }

    private fun formatTag(tag: String): String {
        return "$GLOBAL_TAG_PREFIX$tag"
    }

    fun d(tag: String, message: String) {
        Timber.tag(formatTag(tag)).d(message)
    }

    fun i(tag: String, message: String) {
        Timber.tag(formatTag(tag)).i(message)
    }

    fun w(tag: String, message: String) {
        Timber.tag(formatTag(tag)).w(message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Timber.tag(formatTag(tag)).e(throwable, message)
        
        // Remote Logging (Non-blocking)
        loggingScope.launch {
            try {
                reportRemoteError(tag, message, throwable)
            } catch (e: Exception) {
                // Fail silently to avoid infinite recursion
                Log.e("Logger", "Failed to report remote error", e)
            }
        }
    }

    private suspend fun reportRemoteError(tag: String, message: String, throwable: Throwable?) {
        // Only log to Supabase in non-debug or if specifically enabled
        // For now, let's allow it if Supabase is initialized.
        
        val stackTrace = throwable?.stackTraceToString()
        val contextMap = mapOf(
            "tag" to tag,
            "device" to android.os.Build.MODEL,
            "version" to com.curbos.pos.BuildConfig.VERSION_NAME
        )
        
        val entry = RemoteLogEntry(
            error_message = message,
            stack_trace = stackTrace,
            context = json.encodeToString(contextMap),
            severity = "high", // Critical errors from Android usually high
            category = "client",
            endpoint = tag
        )

        // Use SupabaseManager safely
        com.curbos.pos.data.remote.SupabaseManager.logRemoteError(entry)
    }

    @Serializable
    data class RemoteLogEntry(
        val error_message: String,
        val stack_trace: String? = null,
        val context: String? = null,
        val severity: String = "medium",
        val category: String = "client",
        val endpoint: String? = null
    )
}
