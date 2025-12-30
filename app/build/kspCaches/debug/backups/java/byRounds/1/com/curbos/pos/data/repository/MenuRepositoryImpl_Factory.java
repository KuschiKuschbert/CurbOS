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
public final class MenuRepositoryImpl_Factory implements Factory<MenuRepositoryImpl> {
  @Override
  public MenuRepositoryImpl get() {
    return newInstance();
  }

  public static MenuRepositoryImpl_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MenuRepositoryImpl newInstance() {
    return new MenuRepositoryImpl();
  }

  private static final class InstanceHolder {
    private static final MenuRepositoryImpl_Factory INSTANCE = new MenuRepositoryImpl_Factory();
  }
}
