package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class DeleteTimer @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val timerRepository: TimerRepository,
    private val schedulerRepository: SchedulerRepository,
    private val schedulerExecutor: SchedulerExecutor,
    private val timerStampRepository: TimerStampRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<Int, Unit>(dispatcher) {
    override suspend fun create(params: Int) {
        if (params != TimerEntity.NULL_ID) {
            schedulerRepository.schedulersWithTimerId(params).forEach {
                schedulerExecutor.cancel(it)
                schedulerRepository.delete(it.id)
            }
            timerStampRepository.deleteWithTimerId(params)
            timerRepository.delete(params)

            appDataRepository.notifyDataChanged()
        }
    }
}
