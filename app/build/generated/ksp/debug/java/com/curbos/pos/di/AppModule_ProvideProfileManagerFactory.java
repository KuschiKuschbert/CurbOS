package com.curbos.pos.di;

import android.content.Context;
import com.curbos.pos.data.prefs.ProfileManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideProfileManagerFactory implements Factory<ProfileManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideProfileManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ProfileManager get() {
    return provideProfileManager(contextProvider.get());
  }

  public static AppModule_ProvideProfileManagerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideProfileManagerFactory(contextProvider);
  }

  public static ProfileManager provideProfileManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideProfileManager(context));
  }
}
