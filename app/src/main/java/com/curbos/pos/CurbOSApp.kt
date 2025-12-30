package com.curbos.pos

import android.app.Application

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
        // Explicitly initialize WorkManager if needed, but usually on-demand is fine if manifest is set up.
        // However, with Hilt, on-demand init works if getWorkManagerConfiguration is implemented.
    }

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
