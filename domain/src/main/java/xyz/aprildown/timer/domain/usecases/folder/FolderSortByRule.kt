package xyz.aprildown.timer.domain.usecases.folder

import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.entities.FolderSortBy
import javax.inject.Inject

@Reusable
class FolderSortByRule @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val sharedPreferences: SharedPreferences
) {
    suspend fun get(): FolderSortBy = withContext(dispatcher) {
        if (sharedPreferences.contains(PREF_FOLDER_SORT_BY)) {
            FolderSortBy.values()[sharedPreferences.getInt(PREF_FOLDER_SORT_BY, 0)]
        } else {
            FolderSortBy.AddedOldest
        }
    }

    suspend fun set(value: FolderSortBy): Unit = withContext(dispatcher) {
        sharedPreferences.edit {
            putInt(PREF_FOLDER_SORT_BY, value.ordinal)
        }
    }

    companion object {
        const val PREF_FOLDER_SORT_BY = "pref_folder_sort_by"
    }
}
