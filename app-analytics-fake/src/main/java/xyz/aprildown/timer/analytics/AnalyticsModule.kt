package xyz.aprildown.timer.analytics

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.domain.utils.AppTracker

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    abstract fun bindAppTracker(impl: AppTrackerImpl): AppTracker
}
