package xyz.aprildown.timer.domain.usecases.folder

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import javax.inject.Inject

@Reusable
class DeleteFolder @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: FolderRepository,
    private val appDataRepository: AppDataRepository,
    private val timerRepository: TimerRepository,
    private val deleteTimer: DeleteTimer
) : CoroutinesUseCase<Long, Unit>(dispatcher) {
    override suspend fun create(params: Long) {
        when (params) {
            FolderEntity.FOLDER_DEFAULT -> {
                timerRepository.moveFolderTimersToAnother(
                    originalFolderId = params,
                    targetFolderId = FolderEntity.FOLDER_TRASH
                )
            }
            FolderEntity.FOLDER_TRASH -> {
                timerRepository.getTimerInfo(folderId = params).forEach { timerInfo ->
                    // We have to use a UseCase because deleting a timer is complicated.
                    deleteTimer(params = timerInfo.id)
                }
            }
            else -> {
                // User created folders
                timerRepository.moveFolderTimersToAnother(
                    originalFolderId = params,
                    targetFolderId = FolderEntity.FOLDER_TRASH
                )
                repository.deleteFolder(params)
            }
        }

        appDataRepository.notifyDataChanged()
    }
}
