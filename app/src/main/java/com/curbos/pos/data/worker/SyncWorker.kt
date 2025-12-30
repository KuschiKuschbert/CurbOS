package com.curbos.pos.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.curbos.pos.data.TransactionSyncManager

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionSyncManager: TransactionSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Should throw exception on network failure for retry
            val success = transactionSyncManager.processQueueWithResult() 
            
            if (success) Result.success() else Result.retry()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal cancellation by WorkManager, don't log as error
            throw e
        } catch (e: Exception) {
            com.curbos.pos.common.Logger.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }
}
