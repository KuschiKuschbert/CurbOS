package com.curbos.pos.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.app.PendingIntent
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
            com.curbos.pos.common.SnackbarManager.showMessage("Downloading update... ⬇️")
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
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(fileName, context)
                    try {
                        context.unregisterReceiver(this)
                    } catch (e: Exception) { /* already removed */ }
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context, 
            onComplete, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), 
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(fileName: String, context: Context) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) return

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        
        // API 31+ (Android 12) supports unattended updates if certain conditions are met
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        try {
            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)
            
            file.inputStream().use { inputStream ->
                session.openWrite("curbos_install", 0, file.length()).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            val intent = Intent(context, InstallStatusReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
            session.close()
            
            com.curbos.pos.common.Logger.i("UpdateManager", "Update session $sessionId committed")
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("UpdateManager", "Failed to install update", e)
        }
    }
}

/**
 * Receiver to handle the status of the PackageInstaller session.
 * This is outside the main class to be easily registered in Manifest if needed, 
 * or purely used via PendingIntent.
 */
class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        
        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                com.curbos.pos.common.Logger.i("InstallStatus", "Update successful!")
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // If unattended was denied/not supported, fallback to system dialog
                val confirmIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirmIntent)
                }
            }
            else -> {
                com.curbos.pos.common.Logger.e("InstallStatus", "Update failed ($status): $message")
            }
        }
    }
}
