package xyz.aprildown.timer.presentation.di

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.presentation.R
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
internal object ViewModelModule {
    const val DEFAULT_TIMER_NAME = "default_timer_name"

    @Provides
    @Named(DEFAULT_TIMER_NAME)
    fun provideDefaultTimerName(context: Application): String {
        return context.getString(R.string.edit_default_timer_name)
    }
}
