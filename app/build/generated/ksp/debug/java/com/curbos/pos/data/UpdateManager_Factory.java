package com.curbos.pos.data;

import android.content.Context;
import com.curbos.pos.data.remote.GithubApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class UpdateManager_Factory implements Factory<UpdateManager> {
  private final Provider<Context> contextProvider;

  private final Provider<GithubApiService> githubServiceProvider;

  public UpdateManager_Factory(Provider<Context> contextProvider,
      Provider<GithubApiService> githubServiceProvider) {
    this.contextProvider = contextProvider;
    this.githubServiceProvider = githubServiceProvider;
  }

  @Override
  public UpdateManager get() {
    return newInstance(contextProvider.get(), githubServiceProvider.get());
  }

  public static UpdateManager_Factory create(Provider<Context> contextProvider,
      Provider<GithubApiService> githubServiceProvider) {
    return new UpdateManager_Factory(contextProvider, githubServiceProvider);
  }

  public static UpdateManager newInstance(Context context, GithubApiService githubService) {
    return new UpdateManager(context, githubService);
  }
}
