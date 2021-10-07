package xyz.aprildown.timer.domain.repositories

import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.domain.entities.AppDataEntity
import javax.inject.Qualifier

interface AppDataRepository {

    interface BackupRepository {
        suspend fun onAppDataChanged()
    }

    @Retention(AnnotationRetention.BINARY)
    @Qualifier
    annotation class BackupRepositoryQualifier

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class BackupRepositoryModule {
        @BindsOptionalOf
        @BackupRepositoryQualifier
        abstract fun bindOptionalBackupRepository(): BackupRepository
    }

    suspend fun collectData(appDataEntity: AppDataEntity): String

    suspend fun unParcelData(data: String): AppDataEntity?

    suspend fun notifyDataChanged()
}
