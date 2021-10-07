package xyz.aprildown.timer.domain.usecases.folder

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class GetFolders @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: FolderRepository
) : CoroutinesUseCase<Unit, List<FolderEntity>>(dispatcher) {
    override suspend fun create(params: Unit): List<FolderEntity> {
        val newList = mutableListOf<FolderEntity>()
        var defaultFolder: FolderEntity? = null
        var trashFolder: FolderEntity? = null
        repository.getFolders().forEach { folder ->
            when (folder.id) {
                FolderEntity.FOLDER_DEFAULT -> defaultFolder = folder
                FolderEntity.FOLDER_TRASH -> trashFolder = folder
                else -> newList += folder
            }
        }
        newList.sortBy { it.name }
        newList.add(0, defaultFolder!!)
        newList += trashFolder!!
        return newList
    }
}
