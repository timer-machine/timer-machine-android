package xyz.aprildown.timer.domain.usecases.scheduler

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class SetSchedulerEnable @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: SchedulerRepository,
    private val executor: SchedulerExecutor,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<SetSchedulerEnable.Params, SetSchedulerEnable.Result>(dispatcher) {

    data class Params(
        val id: Int,
        val enable: Int
    )

    sealed class Result {
        class Scheduled(val time: Long) : Result()
        class Canceled(val count: Int) : Result()
        object Failed : Result()
    }

    /**
     * @return If enable, scheduled job time. If disable, disable count.
     */
    override suspend fun create(params: Params): Result {
        val (id, enable) = params
        var result: Result? = null
        repository.item(id)?.copy(enable = enable)?.let { new ->
            result = when (enable) {
                0 -> executor.cancel(new)
                1 -> executor.schedule(new)
                else -> throw IllegalStateException("Invalid scheduler enable value: $enable")
            }
        }
        repository.setSchedulerEnable(id, enable)
        appDataRepository.notifyDataChanged()
        return result ?: Result.Failed
    }
}
