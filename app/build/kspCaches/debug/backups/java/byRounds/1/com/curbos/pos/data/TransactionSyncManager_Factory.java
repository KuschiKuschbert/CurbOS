package com.curbos.pos.data;

import com.curbos.pos.data.local.PosDao;
import com.curbos.pos.data.p2p.P2PConnectivityManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.coroutines.CoroutineDispatcher;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("com.curbos.pos.di.ApplicationScope")
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
public final class TransactionSyncManager_Factory implements Factory<TransactionSyncManager> {
  private final Provider<PosDao> posDaoProvider;

  private final Provider<P2PConnectivityManager> p2pConnectivityManagerProvider;

  private final Provider<CoroutineScope> externalScopeProvider;

  private final Provider<CoroutineDispatcher> ioDispatcherProvider;

  public TransactionSyncManager_Factory(Provider<PosDao> posDaoProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<CoroutineScope> externalScopeProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    this.posDaoProvider = posDaoProvider;
    this.p2pConnectivityManagerProvider = p2pConnectivityManagerProvider;
    this.externalScopeProvider = externalScopeProvider;
    this.ioDispatcherProvider = ioDispatcherProvider;
  }

  @Override
  public TransactionSyncManager get() {
    return newInstance(posDaoProvider.get(), p2pConnectivityManagerProvider.get(), externalScopeProvider.get(), ioDispatcherProvider.get());
  }

  public static TransactionSyncManager_Factory create(Provider<PosDao> posDaoProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<CoroutineScope> externalScopeProvider,
      Provider<CoroutineDispatcher> ioDispatcherProvider) {
    return new TransactionSyncManager_Factory(posDaoProvider, p2pConnectivityManagerProvider, externalScopeProvider, ioDispatcherProvider);
  }

  public static TransactionSyncManager newInstance(PosDao posDao,
      P2PConnectivityManager p2pConnectivityManager, CoroutineScope externalScope,
      CoroutineDispatcher ioDispatcher) {
    return new TransactionSyncManager(posDao, p2pConnectivityManager, externalScope, ioDispatcher);
  }
}
