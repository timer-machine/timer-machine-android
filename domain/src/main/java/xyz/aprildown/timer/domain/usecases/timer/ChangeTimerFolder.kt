package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class ChangeTimerFolder @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<ChangeTimerFolder.Params, Unit>(dispatcher) {

    data class Params(val timerId: Int, val folderId: Long)

    override suspend fun create(params: Params) {
        repository.changeTimerFolder(
            timerId = params.timerId,
            folderId = params.folderId
        ).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
