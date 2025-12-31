package com.curbos.pos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.curbos.pos.common.Logger

class InstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                Logger.i("InstallStatusReceiver", "Requesting user action")
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmIntent)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                Logger.i("InstallStatusReceiver", "Update installed successfully")
            }
            else -> {
                Logger.e("InstallStatusReceiver", "Update failed: $status - $message")
            }
        }
    }
}
