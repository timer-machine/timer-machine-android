package xyz.aprildown.timer.domain.usecases.notifier

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.StepEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.NotifierRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class SaveNotifier @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: NotifierRepository,
    private val appDataRepository: AppDataRepository
) : CoroutinesUseCase<StepEntity.Step, Boolean>(dispatcher) {
    override suspend fun create(params: StepEntity.Step): Boolean {
        return repository.set(params).also {
            appDataRepository.notifyDataChanged()
        }
    }
}
