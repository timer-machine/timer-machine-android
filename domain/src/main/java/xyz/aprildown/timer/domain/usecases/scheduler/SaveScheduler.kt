package xyz.aprildown.timer.domain.usecases.scheduler

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class SaveScheduler @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository,
    private val executor: SchedulerExecutor,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<SchedulerEntity, SaveScheduler.Result>(dispatcher) {

    data class Result(
        val saved: Boolean,
        val scheduleResult: SetSchedulerEnable.Result? = null,
    )

    override suspend fun create(params: SchedulerEntity): Result {
        if (params.isNull) return Result(saved = false)

        repository.item(params.id)?.let { executor.cancel(it) }
        val saved = repository.save(params)
        val scheduleResult = if (saved && params.enable == 1) {
            executor.schedule(params)
        } else {
            null
        }
        appDataRepository.notifyDataChanged()
        return Result(saved = saved, scheduleResult = scheduleResult)
    }
}
