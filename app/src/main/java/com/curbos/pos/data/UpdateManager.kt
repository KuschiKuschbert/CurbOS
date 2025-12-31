package com.curbos.pos.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import com.curbos.pos.BuildConfig
import com.curbos.pos.common.Logger
import com.curbos.pos.data.remote.GithubApiService
import com.curbos.pos.data.remote.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val githubService: GithubApiService
) {
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    suspend fun checkForUpdate(isDevMode: Boolean): GithubRelease? = withContext(Dispatchers.IO) {
        try {
            val release = if (isDevMode) {
                 githubService.getReleaseByTag("KuschiKuschbert", "CurbOS", "dev")
            } else {
                 githubService.getLatestRelease("KuschiKuschbert", "CurbOS")
            }
            
            val remoteVersion = release.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v")

            if (isDevMode) {
                // Parse timestamps for dev builds
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                
                val releaseDate = sdf.parse(release.publishedAt)?.time ?: 0L
                val installDate = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
                
                // Add buffer (e.g. 5 mins) to avoid clock skew issues, or strict comparison
                // Ideally, download builds are always newer than install time if they are fresh.
                if (releaseDate > installDate) {
                    return@withContext release
                }
                return@withContext null
            }

            if (remoteVersion != currentVersion) {
                return@withContext release
            }
            return@withContext null
        } catch (e: Exception) {
            Logger.e("UpdateManager", "Check failed", e)
            return@withContext null
        }
    }

    suspend fun downloadAndInstall(downloadUrl: String) {
        val fileName = "curbos_update.apk"
        _downloadProgress.value = 0
        
        // Clean up old files
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Downloading CurbOS Update")
            .setDescription("Please wait...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Don't notify completion, we handle it
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        // Monitor Progress
        withContext(Dispatchers.IO) {
            var downloading = true
            while (downloading && isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    
                    if (bytesTotal > 0) {
                        val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                        _downloadProgress.value = progress
                    }
                    
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        downloading = false
                    }
                }
                cursor.close()
                delay(500)
            }
        }

        // Register receiver for when download is complete (redundant check but triggers install)
        // using RECEIVER_EXPORTED for Android 13+ support
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    _downloadProgress.value = 100
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
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(fileName: String, context: Context) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (!file.exists()) {
             Logger.e("UpdateManager", "File not found for install: $fileName")
             return
        }

        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        
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
            
            Logger.i("UpdateManager", "Update session $sessionId committed")
        } catch (e: Exception) {
            Logger.e("UpdateManager", "Failed to install update", e)
        }
    }
}
