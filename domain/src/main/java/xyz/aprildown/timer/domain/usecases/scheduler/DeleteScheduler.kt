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
class DeleteScheduler @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository,
    private val executor: SchedulerExecutor,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<Int, Unit>(dispatcher) {
    override suspend fun create(params: Int) {
        if (params != SchedulerEntity.NULL_ID) {
            // Cancel the scheduler before deleting it.
            repository.item(params)?.let {
                executor.cancel(it)
            }
            repository.delete(params)

            appDataRepository.notifyDataChanged()
        }
    }
}
