package com.curbos.pos.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  @Override
  public AuthRepositoryImpl get() {
    return newInstance();
  }

  public static AuthRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static AuthRepositoryImpl newInstance() {
    return new AuthRepositoryImpl();
  }

  private static final class InstanceHolder {
    private static final AuthRepositoryImpl_Factory INSTANCE = new AuthRepositoryImpl_Factory();
  }
}
