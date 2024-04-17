package xyz.aprildown.timer.app.backup

import androidx.compose.runtime.Immutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okio.Source
import okio.buffer
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
internal class Import2ViewModel @Inject constructor(
    private val importAppData: ImportAppData,
    private val notifyDataChanged: NotifyDataChanged,
) : BaseBackupViewModel<Import2ViewModel.ReadableContent>() {
    @Immutable
    data class ImportScreen(
        val wipe: Boolean = false,
        val onWipeChanged: (Boolean) -> Unit,
    )

    class ReadableContent(val getSource: () -> Source) : Screen.Content

    private val _importScreen: MutableStateFlow<ImportScreen> = MutableStateFlow(
        ImportScreen(
            onWipeChanged = ::onWipeChanged,
        )
    )
    val importScreen: StateFlow<ImportScreen> = _importScreen.asStateFlow()

    private fun onWipeChanged(wipe: Boolean) {
        _importScreen.update { it.copy(wipe = wipe) }
    }

    override suspend fun backup(screen: Screen<ReadableContent>): Fruit<Unit> {
        return try {
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
            Fruit.Ripe(Unit)
        } catch (e: IOException) {
            Fruit.Rotten(e)
        }
    }
}
