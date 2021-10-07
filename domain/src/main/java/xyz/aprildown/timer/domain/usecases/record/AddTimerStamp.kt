package xyz.aprildown.timer.domain.usecases.record

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class AddTimerStamp @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerStampRepository,
    private val timerRepository: TimerRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<TimerStampEntity, Int>(dispatcher) {
    override suspend fun create(params: TimerStampEntity): Int {
        return if (timerRepository.getTimerInfoByTimerId(params.timerId) != null) {
            repository.add(params).also {
                appDataRepository.notifyDataChanged()
            }
        } else {
            TimerStampEntity.NULL_ID
        }
    }
}
