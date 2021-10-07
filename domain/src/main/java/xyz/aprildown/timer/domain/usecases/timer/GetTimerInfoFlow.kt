package xyz.aprildown.timer.domain.usecases.timer

import dagger.Lazy
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import javax.inject.Inject

@Reusable
class GetTimerInfoFlow @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository,
    private val timerStampRepository: Lazy<TimerStampRepository>,
) {
    fun get(folderId: Long, sortBy: FolderSortBy): Flow<List<TimerInfo>> {
        return repository.getTimerInfoFlow(folderId)
            .map { it.sort(timerStampRepository, sortBy) }
            .flowOn(dispatcher)
    }
}
