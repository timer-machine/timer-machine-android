package xyz.aprildown.timer.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.data.db.FolderDao
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.timer.data.db.SchedulerDao
import xyz.aprildown.timer.data.db.TimerDao
import xyz.aprildown.timer.data.db.TimerStampDao
import xyz.aprildown.timer.data.repositories.AppDataRepositoryImpl
import xyz.aprildown.timer.data.repositories.FolderRepositoryImpl
import xyz.aprildown.timer.data.repositories.NotifierRepositoryImpl
import xyz.aprildown.timer.data.repositories.PreferencesRepoImpl
import xyz.aprildown.timer.data.repositories.SampleTimerRepositoryImpl
import xyz.aprildown.timer.data.repositories.SchedulerExecutorImpl
import xyz.aprildown.timer.data.repositories.SchedulerRepositoryImpl
import xyz.aprildown.timer.data.repositories.TimerRepositoryImpl
import xyz.aprildown.timer.data.repositories.TimerStampRepositoryImpl
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.repositories.SampleTimerRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepoModule {

    @Binds
    abstract fun bindTimerRepo(repo: TimerRepositoryImpl): TimerRepository

    @Binds
    abstract fun bindFolderRepo(repo: FolderRepositoryImpl): FolderRepository

    @Binds
    abstract fun bindSchedulerRepo(repo: SchedulerRepositoryImpl): SchedulerRepository

    @Binds
    abstract fun bindSchedulerExecutor(executor: SchedulerExecutorImpl): SchedulerExecutor

    @Binds
    abstract fun bindNotifierRepo(repo: NotifierRepositoryImpl): NotifierRepository

    @Binds
    abstract fun bindAppDataRepo(repo: AppDataRepositoryImpl): AppDataRepository

    @Binds
    abstract fun bindTimerStampRepo(repo: TimerStampRepositoryImpl): TimerStampRepository

    @Binds
    abstract fun bindSampleTimersStampRepo(repo: SampleTimerRepositoryImpl): SampleTimerRepository

    @Binds
    abstract fun bindPreferencesRepo(repo: PreferencesRepoImpl): PreferencesRepository

    companion object {
        @Reusable
        @Provides
        fun provideTimerDao(database: MachineDatabase): TimerDao {
            return database.timerDao()
        }

        @Reusable
        @Provides
        fun provideFolderDao(database: MachineDatabase): FolderDao {
            return database.folderDao()
        }

        @Reusable
        @Provides
        fun provideSchedulerDao(database: MachineDatabase): SchedulerDao {
            return database.schedulerDao()
        }

        @Reusable
        @Provides
        fun provideTimerStampDao(database: MachineDatabase): TimerStampDao {
            return database.timerStampDao()
        }
    }
}
