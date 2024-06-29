package xyz.aprildown.timer.app.backup

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okio.Source
import okio.buffer
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged
import javax.inject.Inject

@HiltViewModel
internal class ImportViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val importAppData: ImportAppData,
    private val notifyDataChanged: NotifyDataChanged,
) : BaseBackupViewModel<ImportViewModel.ReadableContent>(mainDispatcher) {
    data class ImportScreen(
        val wipe: Boolean = false,
        val onWipeChange: (Boolean) -> Unit,
    )

    class ReadableContent(val getSource: () -> Source) : Screen.Content

    private val _importScreen: MutableStateFlow<ImportScreen> = MutableStateFlow(
        ImportScreen(
            onWipeChange = ::onWipeChange,
        )
    )
    val importScreen: StateFlow<ImportScreen> = _importScreen.asStateFlow()

    private fun onWipeChange(wipe: Boolean) {
        _importScreen.update { it.copy(wipe = wipe) }
    }

    override suspend fun backup(screen: Screen<ReadableContent>) {
        importAppData(
            ImportAppData.Params(
                data = checkNotNull(screen.content).getSource().use { it.buffer().readUtf8() },
                wipeFirst = _importScreen.value.wipe,
                importTimers = screen.includeTimers,
                importTimerStamps = screen.includeRecords,
                importSchedulers = screen.includeSchedulers,
                importPreferences = screen.includeSettings,
            )
        )
        notifyDataChanged()
    }
}
