package com.curbos.pos;

import com.curbos.pos.data.SyncManager;
import com.curbos.pos.data.UpdateManager;
import com.curbos.pos.data.local.PosDao;
import com.curbos.pos.data.p2p.P2PConnectivityManager;
import com.curbos.pos.data.prefs.ProfileManager;
import com.curbos.pos.data.repository.MenuRepository;
import com.curbos.pos.data.repository.TransactionRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PosDao> posDaoProvider;

  private final Provider<P2PConnectivityManager> p2pManagerProvider;

  private final Provider<SyncManager> syncManagerProvider;

  private final Provider<ProfileManager> profileManagerProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<MenuRepository> menuRepositoryProvider;

  private final Provider<UpdateManager> updateManagerProvider;

  public MainActivity_MembersInjector(Provider<PosDao> posDaoProvider,
      Provider<P2PConnectivityManager> p2pManagerProvider,
      Provider<SyncManager> syncManagerProvider, Provider<ProfileManager> profileManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<MenuRepository> menuRepositoryProvider,
      Provider<UpdateManager> updateManagerProvider) {
    this.posDaoProvider = posDaoProvider;
    this.p2pManagerProvider = p2pManagerProvider;
    this.syncManagerProvider = syncManagerProvider;
    this.profileManagerProvider = profileManagerProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.menuRepositoryProvider = menuRepositoryProvider;
    this.updateManagerProvider = updateManagerProvider;
  }

  public static MembersInjector<MainActivity> create(Provider<PosDao> posDaoProvider,
      Provider<P2PConnectivityManager> p2pManagerProvider,
      Provider<SyncManager> syncManagerProvider, Provider<ProfileManager> profileManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<MenuRepository> menuRepositoryProvider,
      Provider<UpdateManager> updateManagerProvider) {
    return new MainActivity_MembersInjector(posDaoProvider, p2pManagerProvider, syncManagerProvider, profileManagerProvider, transactionRepositoryProvider, menuRepositoryProvider, updateManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPosDao(instance, posDaoProvider.get());
    injectP2pManager(instance, p2pManagerProvider.get());
    injectSyncManager(instance, syncManagerProvider.get());
    injectProfileManager(instance, profileManagerProvider.get());
    injectTransactionRepository(instance, transactionRepositoryProvider.get());
    injectMenuRepository(instance, menuRepositoryProvider.get());
    injectUpdateManager(instance, updateManagerProvider.get());
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.posDao")
  public static void injectPosDao(MainActivity instance, PosDao posDao) {
    instance.posDao = posDao;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.p2pManager")
  public static void injectP2pManager(MainActivity instance, P2PConnectivityManager p2pManager) {
    instance.p2pManager = p2pManager;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.syncManager")
  public static void injectSyncManager(MainActivity instance, SyncManager syncManager) {
    instance.syncManager = syncManager;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.profileManager")
  public static void injectProfileManager(MainActivity instance, ProfileManager profileManager) {
    instance.profileManager = profileManager;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.transactionRepository")
  public static void injectTransactionRepository(MainActivity instance,
      TransactionRepository transactionRepository) {
    instance.transactionRepository = transactionRepository;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.menuRepository")
  public static void injectMenuRepository(MainActivity instance, MenuRepository menuRepository) {
    instance.menuRepository = menuRepository;
  }

  @InjectedFieldSignature("com.curbos.pos.MainActivity.updateManager")
  public static void injectUpdateManager(MainActivity instance, UpdateManager updateManager) {
    instance.updateManager = updateManager;
  }
}
