package xyz.aprildown.timer.domain.usecases.data

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.AppPreferencesProvider
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class ImportAppData @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val appDataRepository: AppDataRepository,
    private val folderRepo: FolderRepository,
    private val timerRepo: TimerRepository,
    private val notifierRepo: NotifierRepository,
    private val timerStampRepository: TimerStampRepository,
    private val schedulerRepo: SchedulerRepository,
    private val appPreferencesProvider: AppPreferencesProvider,
) : CoroutinesUseCase<ImportAppData.Params, Unit>(dispatcher) {

    data class Params(
        val data: String,
        val wipeFirst: Boolean,
        val importTimers: Boolean,
        val importTimerStamps: Boolean,
        val importSchedulers: Boolean,
        val importPreferences: Boolean,
    ) {
        init {
            require(!(importTimerStamps && !importTimers)) {
                "TimerStamps must be imported along with timers"
            }
            require(!(importSchedulers && !importTimers)) {
                "Schedulers must be imported along with timers"
            }
        }
    }

    override suspend fun create(params: Params) {
        if (params.wipeFirst) {
            timerStampRepository.getAll().forEach { timerStampRepository.delete(it.id) }
            schedulerRepo.items().forEach { schedulerRepo.delete(it.id) }
            timerRepo.items().forEach { timerRepo.delete(it.id) }
            notifierRepo.set(null)
            folderRepo.getFolders().forEach {
                if (!it.isDefault && !it.isTrash) {
                    folderRepo.deleteFolder(it.id)
                }
            }
        }

        val result = appDataRepository.unParcelData(params.data) ?: return

        if (params.importPreferences) {
            appPreferencesProvider.applyAppPreferences(result.prefs)
        }

        if (!params.importTimers) return

        val oldNewFolderIdMap = mutableMapOf<Long, Long>()
        oldNewFolderIdMap[FolderEntity.FOLDER_DEFAULT] = FolderEntity.FOLDER_DEFAULT
        oldNewFolderIdMap[FolderEntity.FOLDER_TRASH] = FolderEntity.FOLDER_TRASH
        result.folders.forEach {
            if (!it.isDefault && !it.isTrash) {
                oldNewFolderIdMap[it.id] =
                    folderRepo.addFolder(it.copy(id = FolderEntity.NEW_ID))
            }
        }

        val newTimerStamps = mutableListOf<TimerStampEntity>()
        if (params.importTimerStamps) {
            result.timerStamps.forEach {
                newTimerStamps.add(it.copy(id = TimerStampEntity.NEW_ID))
            }
        }

        val newSchedulers = mutableListOf<SchedulerEntity>()
        if (params.importSchedulers) {
            result.schedulers.forEach {
                newSchedulers.add(it.copy(id = SchedulerEntity.NEW_ID, enable = 0))
            }
        }

        val newTimerIds = mutableListOf<Int>()
        result.timers.forEach { timer ->
            val oldTimerId = timer.id
            val newTimerId = timerRepo.add(
                timer.copy(
                    id = TimerEntity.NEW_ID,
                    folderId = oldNewFolderIdMap[timer.folderId] ?: FolderEntity.FOLDER_DEFAULT
                )
            )
            newTimerIds.add(newTimerId)

            for (index in 0 until newTimerStamps.size) {
                val newTimerStamp = newTimerStamps[index]
                if (newTimerStamp.timerId == oldTimerId) {
                    newTimerStamps[index] = newTimerStamp.copy(timerId = newTimerId)
                }
            }

            for (index in 0 until newSchedulers.size) {
                val newScheduler = newSchedulers[index]
                if (newScheduler.timerId == oldTimerId) {
                    newSchedulers[index] = newScheduler.copy(timerId = newTimerId)
                }
            }
        }

        newSchedulers.forEach {
            if (it.timerId in newTimerIds) {
                schedulerRepo.add(it)
            }
        }
        newTimerStamps.forEach {
            if (it.timerId in newTimerIds) {
                timerStampRepository.add(it)
            }
        }

        notifierRepo.set(result.notifier)
    }
}
