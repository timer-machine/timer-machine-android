package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class GetTimer @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository
) : CoroutinesUseCase<Int, TimerEntity?>(dispatcher) {
    override suspend fun create(params: Int): TimerEntity? {
        return if (params == TimerEntity.NULL_ID) {
            null
        } else {
            repository.item(params)
        }
    }
}
