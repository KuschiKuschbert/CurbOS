package com.curbos.pos.di;

import com.curbos.pos.data.local.AppDatabase;
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
public final class AppModule_ProvidePosDaoFactory implements Factory<PosDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvidePosDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PosDao get() {
    return providePosDao(databaseProvider.get());
  }

  public static AppModule_ProvidePosDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvidePosDaoFactory(databaseProvider);
  }

  public static PosDao providePosDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePosDao(database));
  }
}
