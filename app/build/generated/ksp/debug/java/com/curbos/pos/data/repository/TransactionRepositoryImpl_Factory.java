package com.curbos.pos.data.repository;

import android.content.Context;
import com.curbos.pos.data.TransactionSyncManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TransactionRepositoryImpl_Factory implements Factory<TransactionRepositoryImpl> {
  private final Provider<TransactionSyncManager> transactionSyncManagerProvider;

  private final Provider<Context> contextProvider;

  public TransactionRepositoryImpl_Factory(
      Provider<TransactionSyncManager> transactionSyncManagerProvider,
      Provider<Context> contextProvider) {
    this.transactionSyncManagerProvider = transactionSyncManagerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public TransactionRepositoryImpl get() {
    return newInstance(transactionSyncManagerProvider.get(), contextProvider.get());
  }

  public static TransactionRepositoryImpl_Factory create(
      Provider<TransactionSyncManager> transactionSyncManagerProvider,
      Provider<Context> contextProvider) {
    return new TransactionRepositoryImpl_Factory(transactionSyncManagerProvider, contextProvider);
  }

  public static TransactionRepositoryImpl newInstance(TransactionSyncManager transactionSyncManager,
      Context context) {
    return new TransactionRepositoryImpl(transactionSyncManager, context);
  }
}
