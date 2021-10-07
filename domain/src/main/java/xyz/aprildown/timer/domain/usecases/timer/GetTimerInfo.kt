package xyz.aprildown.timer.domain.usecases.timer

import dagger.Lazy
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import javax.inject.Inject

@Reusable
class GetTimerInfo @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    private val repository: TimerRepository,
    private val timerStampRepository: Lazy<TimerStampRepository>,
) : CoroutinesUseCase<GetTimerInfo.Params, List<TimerInfo>>(dispatcher) {

    data class Params(val folderId: Long, val sortBy: FolderSortBy)

    override suspend fun create(params: Params): List<TimerInfo> {
        return repository.getTimerInfo(params.folderId)
            .sort(timerStampRepository, params.sortBy)
    }
}

internal suspend fun List<TimerInfo>.sort(
    timerStampRepository: Lazy<TimerStampRepository>,
    sortBy: FolderSortBy
): List<TimerInfo> {
    return when (sortBy) {
        FolderSortBy.AddedNewest -> sortedByDescending { it.id }
        FolderSortBy.AddedOldest -> sortedBy { it.id }
        FolderSortBy.AToZ -> sortedBy { it.name }
        FolderSortBy.ZToA -> sortedByDescending { it.name }
        FolderSortBy.RunNewest -> {
            sortedByDescending {
                runBlocking {
                    timerStampRepository.get().getRecentOne(it.id)?.end ?: 0
                }
            }
        }
        FolderSortBy.RunOldest -> {
            sortedBy {
                runBlocking {
                    timerStampRepository.get().getRecentOne(it.id)?.end ?: 0
                }
            }
        }
    }
}
