package com.jarvis.mark39.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.jarvis.mark39.data.local.JarvisDatabase
import com.jarvis.mark39.data.local.MemoryDao
import com.jarvis.mark39.data.local.MessageDao
import com.jarvis.mark39.data.local.TaskDao
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
    fun provideDatabase(@ApplicationContext context: Context): JarvisDatabase =
        Room.databaseBuilder(context, JarvisDatabase::class.java, "jarvis_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideMessageDao(db: JarvisDatabase): MessageDao = db.messageDao()
    @Provides fun provideMemoryDao(db: JarvisDatabase): MemoryDao = db.memoryDao()
    @Provides fun provideTaskDao(db: JarvisDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
