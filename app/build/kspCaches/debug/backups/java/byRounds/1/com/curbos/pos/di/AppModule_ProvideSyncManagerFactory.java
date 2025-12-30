package com.curbos.pos.di;

import com.curbos.pos.data.SyncManager;
import com.curbos.pos.data.local.PosDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideSyncManagerFactory implements Factory<SyncManager> {
  private final Provider<PosDao> posDaoProvider;

  public AppModule_ProvideSyncManagerFactory(Provider<PosDao> posDaoProvider) {
    this.posDaoProvider = posDaoProvider;
  }

  @Override
  public SyncManager get() {
    return provideSyncManager(posDaoProvider.get());
  }

  public static AppModule_ProvideSyncManagerFactory create(Provider<PosDao> posDaoProvider) {
    return new AppModule_ProvideSyncManagerFactory(posDaoProvider);
  }

  public static SyncManager provideSyncManager(PosDao posDao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSyncManager(posDao));
  }
}
