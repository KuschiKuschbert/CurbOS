package com.curbos.pos.di

import android.content.Context
import androidx.room.Room
import com.curbos.pos.data.TransactionSyncManager
import com.curbos.pos.data.local.AppDatabase
import com.curbos.pos.data.local.PosDao
import com.curbos.pos.data.p2p.P2PConnectivityManager
import com.curbos.pos.data.prefs.ProfileManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "curbos-pos-db"
        )
        // .fallbackToDestructiveMigration() // REMOVED: Never wipe data on updates.
        .build()
    }

    @Provides
    @Singleton
    fun providePosDao(database: AppDatabase): PosDao {
        return database.posDao()
    }

    @Provides
    @Singleton
    fun provideP2PConnectivityManager(@ApplicationContext context: Context): P2PConnectivityManager {
        return P2PConnectivityManager(context)
    }



    @Provides
    @Singleton
    fun provideProfileManager(@ApplicationContext context: Context): ProfileManager {
        return ProfileManager(context)
    }

    @Provides
    @Singleton
    fun provideSyncManager(posDao: PosDao): com.curbos.pos.data.SyncManager {
        return com.curbos.pos.data.SyncManager(posDao)
    }

    @Provides
    @Singleton
    fun provideGithubApiService(): com.curbos.pos.data.remote.GithubApiService {
        return retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.curbos.pos.data.remote.GithubApiService::class.java)
    }
}
