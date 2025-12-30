package com.curbos.pos.ui.viewmodel;

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
public final class CustomerDisplayViewModel_Factory implements Factory<CustomerDisplayViewModel> {
  private final Provider<P2PConnectivityManager> p2pConnectivityManagerProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public CustomerDisplayViewModel_Factory(
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.p2pConnectivityManagerProvider = p2pConnectivityManagerProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public CustomerDisplayViewModel get() {
    return newInstance(p2pConnectivityManagerProvider.get(), transactionRepositoryProvider.get());
  }

  public static CustomerDisplayViewModel_Factory create(
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new CustomerDisplayViewModel_Factory(p2pConnectivityManagerProvider, transactionRepositoryProvider);
  }

  public static CustomerDisplayViewModel newInstance(P2PConnectivityManager p2pConnectivityManager,
      TransactionRepository transactionRepository) {
    return new CustomerDisplayViewModel(p2pConnectivityManager, transactionRepository);
  }
}
