package xyz.aprildown.timer.domain.usecases.data

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class ExportAppData @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val appDataRepository: AppDataRepository,
    private val folderRepo: FolderRepository,
    private val timerRepo: TimerRepository,
    private val notifierRepo: NotifierRepository,
    private val timerStampRepo: TimerStampRepository,
    private val schedulerRepo: SchedulerRepository
) : CoroutinesUseCase<ExportAppData.Params, String>(dispatcher) {

    class Params(
        val exportTimers: Boolean,
        val exportTimerStamps: Boolean,
        val exportSchedulers: Boolean,
        val prefs: Map<String, String>
    ) {
        init {
            require(!(exportTimerStamps && !exportTimers)) {
                "TimerStamps must be exported along with timers"
            }
            require(!(exportSchedulers && !exportTimers)) {
                "Schedulers must be exported along with timers"
            }
        }
    }

    override suspend fun create(params: Params): String {
        val exportTimers = params.exportTimers
        return appDataRepository.collectData(
            AppDataEntity(
                folders = if (exportTimers) folderRepo.getFolders() else emptyList(),
                timers = if (exportTimers) timerRepo.items() else emptyList(),
                notifier = if (exportTimers) notifierRepo.get() else null,
                timerStamps = if (params.exportTimerStamps) {
                    timerStampRepo.getAll()
                } else {
                    emptyList()
                },
                schedulers =
                if (params.exportSchedulers) schedulerRepo.items().map {
                    it.copy(enable = 0)
                } else emptyList(),
                prefs = params.prefs
            )
        )
    }
}
