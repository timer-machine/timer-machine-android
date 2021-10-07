package io.github.deweyreed.timer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.app.base.data.FlavorData
import xyz.aprildown.timer.app.base.ui.FlavorUiInjector
import xyz.aprildown.timer.app.base.ui.FlavorUiInjectorQualifier
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.flavor.google.FlavorUiInjectorImpl
import xyz.aprildown.timer.flavor.google.backup.BackupRepoImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorModule {

    @Binds
    abstract fun bindFlavorData(impl: FlavorDataImpl): FlavorData

    @Binds
    @AppDataRepository.BackupRepositoryQualifier
    abstract fun bindBackupRepo(impl: BackupRepoImpl): AppDataRepository.BackupRepository

    @Binds
    @FlavorUiInjectorQualifier
    abstract fun bindFlavorUiInjector(impl: FlavorUiInjectorImpl): FlavorUiInjector
}
