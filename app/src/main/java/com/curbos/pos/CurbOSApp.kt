package com.curbos.pos

import android.app.Application
import com.curbos.pos.data.remote.SupabaseManager

@dagger.hilt.android.HiltAndroidApp
class CurbOSApp : Application(), androidx.work.Configuration.Provider {
    
    @javax.inject.Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    companion object {
        lateinit var instance: CurbOSApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 1. Initialize Modern Logger
        com.curbos.pos.common.Logger.init()
        com.curbos.pos.common.Logger.i("CurbOSApp", "Application starting (Modernized Logging Enabled)")

        // 2. Initialize Supabase
        SupabaseManager.init()

        // 3. Set up Global Coroutine Exception Handler
        setupGlobalErrorHandler()
    }

    private fun setupGlobalErrorHandler() {
        // This catches unhandled exceptions in the CoroutineScope of the app (if any)
        // For truly global (including UI thread crashes), we'd use Thread.setDefaultUncaughtExceptionHandler
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            com.curbos.pos.common.Logger.e("GlobalError", "Unhandled exception caught", throwable)
        }
        
        // We can also set the default uncaught exception handler for the entire process
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.curbos.pos.common.Logger.e("Crash", "FATAL CRASH on thread ${thread.name}", throwable)
            // Still let the original handler handle it (usually shows the crash dialog)
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
