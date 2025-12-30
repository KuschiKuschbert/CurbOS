package com.curbos.pos.ui.viewmodel;

import com.curbos.pos.data.prefs.ProfileManager;
import com.curbos.pos.data.repository.AuthRepository;
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
public final class LoginViewModel_Factory implements Factory<LoginViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ProfileManager> profileManagerProvider;

  public LoginViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileManager> profileManagerProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.profileManagerProvider = profileManagerProvider;
  }

  @Override
  public LoginViewModel get() {
    return newInstance(authRepositoryProvider.get(), profileManagerProvider.get());
  }

  public static LoginViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<ProfileManager> profileManagerProvider) {
    return new LoginViewModel_Factory(authRepositoryProvider, profileManagerProvider);
  }

  public static LoginViewModel newInstance(AuthRepository authRepository,
      ProfileManager profileManager) {
    return new LoginViewModel(authRepository, profileManager);
  }
}
