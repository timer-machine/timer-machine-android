package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class FindTimerInfo @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository
) : CoroutinesUseCase<Int, TimerInfo?>(dispatcher) {
    override suspend fun create(params: Int): TimerInfo? {
        if (params == TimerEntity.NULL_ID) return null
        return repository.getTimerInfoByTimerId(params)
    }
}
