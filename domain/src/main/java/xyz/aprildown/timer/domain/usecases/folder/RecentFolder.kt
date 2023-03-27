package xyz.aprildown.timer.domain.usecases.folder

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import javax.inject.Inject

@Reusable
class RecentFolder @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend fun get(): Long = withContext(dispatcher) {
        preferencesRepository.getLong(PREF_RECENT_FOLDER, FolderEntity.FOLDER_DEFAULT)
    }

    suspend fun set(value: Long): Unit = withContext(dispatcher) {
        if (value == FolderEntity.FOLDER_TRASH) return@withContext

        preferencesRepository.setLong(PREF_RECENT_FOLDER, value)
    }

    companion object {
        const val PREF_RECENT_FOLDER = "pref_recent_folder"
    }
}
