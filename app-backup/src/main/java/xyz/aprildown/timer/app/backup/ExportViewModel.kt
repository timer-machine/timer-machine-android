package xyz.aprildown.timer.app.backup

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import okio.Sink
import okio.buffer
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import javax.inject.Inject

@HiltViewModel
internal class ExportViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
    private val exportAppData: ExportAppData,
) : BaseBackupViewModel<ExportViewModel.WritableContent>(mainDispatcher) {
    class WritableContent(
        val getSink: () -> Sink,
        val delete: () -> Unit,
    ) : Screen.Content

    override suspend fun backup(screen: Screen<WritableContent>) {
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
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onCleared() {
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
