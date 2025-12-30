package com.curbos.pos.ui.viewmodel;

import com.curbos.pos.data.local.PosDao;
import com.curbos.pos.data.prefs.ProfileManager;
import com.curbos.pos.data.repository.MenuRepository;
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
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  private final Provider<PosDao> posDaoProvider;

  private final Provider<ProfileManager> profileManagerProvider;

  private final Provider<TransactionRepository> transactionRepositoryProvider;

  private final Provider<MenuRepository> menuRepositoryProvider;

  public AdminViewModel_Factory(Provider<PosDao> posDaoProvider,
      Provider<ProfileManager> profileManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<MenuRepository> menuRepositoryProvider) {
    this.posDaoProvider = posDaoProvider;
    this.profileManagerProvider = profileManagerProvider;
    this.transactionRepositoryProvider = transactionRepositoryProvider;
    this.menuRepositoryProvider = menuRepositoryProvider;
  }

  @Override
  public AdminViewModel get() {
    return newInstance(posDaoProvider.get(), profileManagerProvider.get(), transactionRepositoryProvider.get(), menuRepositoryProvider.get());
  }

  public static AdminViewModel_Factory create(Provider<PosDao> posDaoProvider,
      Provider<ProfileManager> profileManagerProvider,
      Provider<TransactionRepository> transactionRepositoryProvider,
      Provider<MenuRepository> menuRepositoryProvider) {
    return new AdminViewModel_Factory(posDaoProvider, profileManagerProvider, transactionRepositoryProvider, menuRepositoryProvider);
  }

  public static AdminViewModel newInstance(PosDao posDao, ProfileManager profileManager,
      TransactionRepository transactionRepository, MenuRepository menuRepository) {
    return new AdminViewModel(posDao, profileManager, transactionRepository, menuRepository);
  }
}
