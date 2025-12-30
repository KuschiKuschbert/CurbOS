package com.curbos.pos.di;

import android.content.Context;
import com.curbos.pos.data.p2p.P2PConnectivityManager;
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
public final class AppModule_ProvideP2PConnectivityManagerFactory implements Factory<P2PConnectivityManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideP2PConnectivityManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public P2PConnectivityManager get() {
    return provideP2PConnectivityManager(contextProvider.get());
  }

  public static AppModule_ProvideP2PConnectivityManagerFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideP2PConnectivityManagerFactory(contextProvider);
  }

  public static P2PConnectivityManager provideP2PConnectivityManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideP2PConnectivityManager(context));
  }
}
