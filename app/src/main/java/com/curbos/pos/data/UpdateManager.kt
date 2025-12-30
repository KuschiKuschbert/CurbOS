package com.curbos.pos.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.curbos.pos.BuildConfig
import com.curbos.pos.data.remote.GithubApiService
import com.curbos.pos.data.remote.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubService: GithubApiService
) {

    suspend fun checkForUpdate(isDevMode: Boolean): GithubRelease? = withContext(Dispatchers.IO) {
        try {
            val release = if (isDevMode) {
                 githubService.getReleaseByTag("KuschiKuschbert", "CurbOS", "nightly")
            } else {
                 githubService.getLatestRelease("KuschiKuschbert", "CurbOS")
            }
            
            // remove 'v' prefix if present for comparison
            val remoteVersion = release.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v")

            if (isDevMode) {
                // In Dev Mode, always offer update if it's the 'nightly' tag, 
                // assuming the user wants to reinstall the latest nightly.
                // Or compare published_at timestamps if available. For now, simple return.
                return@withContext release
            }

            if (remoteVersion != currentVersion) {
                // Ideally use a semver comparison library, but simple string inequality 
                // is enough to signal "something is different"
                return@withContext release
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun downloadAndInstall(downloadUrl: String) {
        val fileName = "curbos_update.apk"
        
        // Visual Feedback
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            com.curbos.pos.common.SnackbarManager.showMessage("Downloading update... Check notification bar ⬇️")
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Downloading CurbOS Update")
            .setDescription("Please wait...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // Register receiver for when download is complete
        // We use the application context here via the injected 'context' field
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    // Start installation immediately
                    installApk(fileName, context)
                    
                    // Unregister self to avoid leaks
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Ignore if already unregistered
                    }
                }
            }
        }
        
        // Register receiver with the Application Context
        ContextCompat.registerReceiver(
            context, 
            onComplete, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(fileName: String, context: Context) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
