package com.curbos.pos.di;

import com.curbos.pos.data.remote.GithubApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideGithubApiServiceFactory implements Factory<GithubApiService> {
  @Override
  public GithubApiService get() {
    return provideGithubApiService();
  }

  public static AppModule_ProvideGithubApiServiceFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GithubApiService provideGithubApiService() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGithubApiService());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGithubApiServiceFactory INSTANCE = new AppModule_ProvideGithubApiServiceFactory();
  }
}
