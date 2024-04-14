package xyz.aprildown.timer.app.backup

import androidx.compose.runtime.Immutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import xyz.aprildown.timer.domain.usecases.Fruit
import javax.inject.Inject

@HiltViewModel
internal class Import2ViewModel @Inject constructor() : BaseBackupViewModel() {
    @Immutable
    data class ImportScreen(
        val wipe: Boolean = false,
        val onWipeChanged: (Boolean) -> Unit,
    )

    private val _importScreen: MutableStateFlow<ImportScreen> = MutableStateFlow(
        ImportScreen(
            onWipeChanged = ::onWipeChanged,
        )
    )
    val importScreen: StateFlow<ImportScreen> = _importScreen.asStateFlow()

    private fun onWipeChanged(wipe: Boolean) {
        _importScreen.update { it.copy(wipe = wipe) }
    }

    override suspend fun backup(screen: Screen): Fruit<Unit> {
        delay(5000)
        return Fruit.Rotten("Hey")
    }
}
