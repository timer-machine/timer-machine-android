package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.SampleTimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class SampleTimerProvider @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SampleTimerRepository
) : CoroutinesUseCase<Int, TimerEntity>(dispatcher) {
    override suspend fun create(params: Int): TimerEntity {
        return repository.getSampleTimer(params)
    }

    companion object {
        const val NO_SAMPLE = 0

        const val TEMPLATE_ONE_STAGE = 1
        const val TEMPLATE_TWO_STAGES = 2
        const val TEMPLATE_THREE_STAGES = 3
    }
}
