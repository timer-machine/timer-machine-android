package xyz.aprildown.timer.domain.usecases.folder

import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderEntity
import javax.inject.Inject

@Reusable
class RecentFolder @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun get(): Long = withContext(dispatcher) {
        sharedPreferences.getLong(PREF_RECENT_FOLDER, FolderEntity.FOLDER_DEFAULT)
    }

    suspend fun set(value: Long): Unit = withContext(dispatcher) {
        if (value == FolderEntity.FOLDER_TRASH) return@withContext

        sharedPreferences.edit {
            putLong(PREF_RECENT_FOLDER, value)
        }
    }

    companion object {
        const val PREF_RECENT_FOLDER = "pref_recent_folder"
    }
}
