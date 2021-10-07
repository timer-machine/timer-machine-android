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
class UpdateFolder @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: FolderRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<FolderEntity, Boolean>(dispatcher) {
    override suspend fun create(params: FolderEntity): Boolean {
        val folderId = params.id

        if (folderId == FolderEntity.FOLDER_DEFAULT || folderId == FolderEntity.FOLDER_TRASH) return false

        return repository.updateFolder(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
