package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class AddTimer @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<TimerEntity, Int>(dispatcher) {
    override suspend fun create(params: TimerEntity): Int {
        return repository.add(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
