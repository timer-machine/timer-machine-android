package xyz.aprildown.timer.domain.usecases.folder

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import javax.inject.Inject

@Reusable
class FolderSortByRule @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend fun get(): FolderSortBy = withContext(dispatcher) {
        FolderSortBy.values()[
            preferencesRepository.getInt(
                PREF_FOLDER_SORT_BY,
                FolderSortBy.AddedOldest.ordinal
            )
        ]
    }

    suspend fun set(value: FolderSortBy): Unit = withContext(dispatcher) {
        preferencesRepository.setInt(PREF_FOLDER_SORT_BY, value.ordinal)
    }

    companion object {
        const val PREF_FOLDER_SORT_BY = "pref_folder_sort_by"
    }
}
