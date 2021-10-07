package xyz.aprildown.timer.domain.usecases.timer

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.usecases.Fruit
import javax.inject.Inject

@Reusable
class ShareTimer @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val appDataRepository: AppDataRepository,
) {
    suspend fun shareAsString(timers: List<TimerEntity>): Fruit<String> = withContext(dispatcher) {
        try {
            Fruit.Ripe(
                appDataRepository.collectData(
                    AppDataEntity(timers = timers)
                )
            )
        } catch (e: Exception) {
            Fruit.Rotten(e)
        }
    }

    suspend fun receiveFromString(data: String): Fruit<AppDataEntity?> = withContext(dispatcher) {
        try {
            val entity = appDataRepository.unParcelData(data)
            Fruit.Ripe(
                if (entity != null && entity.timers.isNotEmpty()) {
                    AppDataEntity(timers = entity.timers)
                } else {
                    null
                }
            )
        } catch (e: Exception) {
            Fruit.Rotten(e)
        }
    }
}
