package io.github.deweyreed.timer

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.timer.data.di.DataModule
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class]
)
object TestDataModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MachineDatabase {
        return MachineDatabase.createInMemoryDatabase(context)
    }

    @Reusable
    @Provides
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("TestSp", Context.MODE_PRIVATE)
    }
}
