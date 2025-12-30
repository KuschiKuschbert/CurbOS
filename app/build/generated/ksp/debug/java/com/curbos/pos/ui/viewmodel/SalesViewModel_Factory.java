package com.curbos.pos.ui.viewmodel;

import com.curbos.pos.data.local.PosDao;
import com.curbos.pos.data.p2p.P2PConnectivityManager;
import com.curbos.pos.data.repository.TransactionRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SalesViewModel_Factory implements Factory<SalesViewModel> {
  private final Provider<PosDao> posDaoProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<P2PConnectivityManager> p2pConnectivityManagerProvider;

  public SalesViewModel_Factory(Provider<PosDao> posDaoProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider) {
    this.posDaoProvider = posDaoProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.p2pConnectivityManagerProvider = p2pConnectivityManagerProvider;
  }

  @Override
  public SalesViewModel get() {
    return newInstance(posDaoProvider.get(), transactionRepositoryProvider.get(), p2pConnectivityManagerProvider.get());
  }

  public static SalesViewModel_Factory create(Provider<PosDao> posDaoProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider) {
    return new SalesViewModel_Factory(posDaoProvider, transactionRepositoryProvider, p2pConnectivityManagerProvider);
  }

  public static SalesViewModel newInstance(PosDao posDao,
      TransactionRepository transactionRepository, P2PConnectivityManager p2pConnectivityManager) {
    return new SalesViewModel(posDao, transactionRepository, p2pConnectivityManager);
  }
}
