package com.curbos.pos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.curbos.pos.data.remote.SupabaseManager

@HiltAndroidApp
class CurbOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Supabase on the Main Thread
        // This is safe because Application.onCreate() is always called on the Main Thread,
        // even if the process is started by a Service or Worker.
        SupabaseManager.init()
    }
}
