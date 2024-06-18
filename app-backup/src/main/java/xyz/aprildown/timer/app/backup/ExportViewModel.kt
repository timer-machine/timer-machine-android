package xyz.aprildown.timer.app.backup

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import okio.Sink
import okio.buffer
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import javax.inject.Inject

@HiltViewModel
internal class ExportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val exportAppData: ExportAppData,
) : BaseBackupViewModel<ExportViewModel.WritableContent>() {
    class WritableContent(
        val getSink: () -> Sink,
        val delete: () -> Unit,
    ) : Screen.Content

    override suspend fun backup(screen: Screen<WritableContent>): Fruit<Unit> {
        return try {
            val string = exportAppData(
                ExportAppData.Params(
                    exportTimers = screen.includeTimers,
                    exportTimerStamps = screen.includeRecords,
                    exportSchedulers = screen.includeSchedulers,
                    exportPreferences = screen.includeSettings,
                )
            )
            checkNotNull(screen.content).getSink().use {
                it.buffer().run {
                    writeUtf8(string)
                    flush()
                }
            }
            savedStateHandle[KEY_HAS_EXPORTED] = true
            Fruit.Ripe(Unit)
        } catch (e: Exception) {
            Fruit.Rotten(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (savedStateHandle.get<Boolean>(KEY_HAS_EXPORTED) != true) {
            try {
                screen.value.content?.delete?.invoke()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        private const val KEY_HAS_EXPORTED = "has_exported"
    }
}
