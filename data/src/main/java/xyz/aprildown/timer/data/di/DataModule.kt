package xyz.aprildown.timer.data.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.tools.helper.safeSharedPreference
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MachineDatabase {
        return MachineDatabase.createPersistentDatabase(context)
    }

    @Reusable
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.safeSharedPreference
    }
}
