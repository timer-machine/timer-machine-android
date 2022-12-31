package xyz.aprildown.timer.domain.usecases.folder

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class AddFolder @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: FolderRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<FolderEntity, Long>(dispatcher) {
    override suspend fun create(params: FolderEntity): Long {
        if (params.isDefault || params.isTrash) return params.id

        require(params.name.isNotBlank())

        return repository.addFolder(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
