package xyz.aprildown.timer.domain.usecases.scheduler

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class AddScheduler @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<SchedulerEntity, Int>(dispatcher) {
    override suspend fun create(params: SchedulerEntity): Int {
        return repository.add(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
