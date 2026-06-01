package com.futureape.kanleme.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.futureape.kanleme.data.local.AppDatabase
import com.futureape.kanleme.data.repository.AppRepository
import com.futureape.kanleme.data.repository.AppRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "kanleme.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver
}
