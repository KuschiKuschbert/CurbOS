package com.curbos.pos.data.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.curbos.pos.data.TransactionSyncManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class SyncWorker_Factory {
  private final Provider<TransactionSyncManager> transactionSyncManagerProvider;

  public SyncWorker_Factory(Provider<TransactionSyncManager> transactionSyncManagerProvider) {
    this.transactionSyncManagerProvider = transactionSyncManagerProvider;
  }

  public SyncWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, transactionSyncManagerProvider.get());
  }

  public static SyncWorker_Factory create(
      Provider<TransactionSyncManager> transactionSyncManagerProvider) {
    return new SyncWorker_Factory(transactionSyncManagerProvider);
  }

  public static SyncWorker newInstance(Context appContext, WorkerParameters workerParams,
      TransactionSyncManager transactionSyncManager) {
    return new SyncWorker(appContext, workerParams, transactionSyncManager);
  }
}
