package com.curbos.pos.ui.viewmodel;

import com.curbos.pos.data.p2p.P2PConnectivityManager;
import com.curbos.pos.data.prefs.ProfileManager;
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
public final class KitchenViewModel_Factory implements Factory<KitchenViewModel> {
  private final Provider<ProfileManager> profileManagerProvider;

  private final Provider<P2PConnectivityManager> p2pConnectivityManagerProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public KitchenViewModel_Factory(Provider<ProfileManager> profileManagerProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.profileManagerProvider = profileManagerProvider;
    this.p2pConnectivityManagerProvider = p2pConnectivityManagerProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public KitchenViewModel get() {
    return newInstance(profileManagerProvider.get(), p2pConnectivityManagerProvider.get(), transactionRepositoryProvider.get());
  }

  public static KitchenViewModel_Factory create(Provider<ProfileManager> profileManagerProvider,
      Provider<P2PConnectivityManager> p2pConnectivityManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new KitchenViewModel_Factory(profileManagerProvider, p2pConnectivityManagerProvider, transactionRepositoryProvider);
  }

  public static KitchenViewModel newInstance(ProfileManager profileManager,
      P2PConnectivityManager p2pConnectivityManager, TransactionRepository transactionRepository) {
    return new KitchenViewModel(profileManager, p2pConnectivityManager, transactionRepository);
  }
}
