package xyz.aprildown.timer.app.backup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.usecases.Fruit

internal abstract class BaseBackupViewModel : ViewModel() {
    @Immutable
    data class Screen(
        val contentLocation: String? = null,
        val contentName: String? = null,

        val includeTimers: Boolean = true,
        val onTimersChange: (Boolean) -> Unit,

        val includeRecords: Boolean = true,
        val onRecordsChange: (Boolean) -> Unit,

        val includeSchedulers: Boolean = true,
        val onSchedulersChange: (Boolean) -> Unit,

        val includeSettings: Boolean = true,
        val onSettingsChange: (Boolean) -> Unit,

        val onBackup: () -> Unit,
        val backupOngoing: Boolean = false,

        val backupErrorMessage: String? = null,
        val consumeBackupError: () -> Unit,
    )

    private val _screen: MutableStateFlow<Screen> = MutableStateFlow(
        Screen(
            onTimersChange = ::onTimersChange,
            onRecordsChange = ::onRecordsChange,
            onSchedulersChange = ::onSchedulersChange,
            onSettingsChange = ::onSettingsChange,
            onBackup = ::onBackup,
            consumeBackupError = ::consumeBackupError,
        )
    )
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun changeContentLocation(location: String, name: String?) {
        _screen.update { it.copy(contentLocation = location, contentName = name) }
    }

    private fun onTimersChange(include: Boolean) {
        _screen.update {
            it.copy(
                includeTimers = include,
                includeRecords = include,
                includeSchedulers = include,
            )
        }
    }

    private fun onRecordsChange(include: Boolean) {
        _screen.update {
            it.copy(
                includeTimers = if (include) true else it.includeTimers,
                includeRecords = include,
            )
        }
    }

    private fun onSchedulersChange(include: Boolean) {
        _screen.update {
            it.copy(
                includeTimers = if (include) true else it.includeTimers,
                includeSchedulers = include,
            )
        }
    }

    private fun onSettingsChange(include: Boolean) {
        _screen.update {
            it.copy(includeSettings = include)
        }
    }

    abstract suspend fun backup(screen: Screen): Fruit<Unit>

    private fun onBackup() {
        val screen = _screen.value
        if (screen.contentLocation.isNullOrBlank()) return
        if (!screen.includeTimers && !screen.includeSettings) return
        if (screen.backupOngoing) return

        viewModelScope.launch {
            _screen.update { it.copy(backupOngoing = true) }
            try {
                when (val fruit = backup(screen)) {
                    is Fruit.Ripe -> Unit
                    is Fruit.Rotten -> {
                        _screen.update {
                            it.copy(backupErrorMessage = fruit.exception.message)
                        }
                    }
                }
            } finally {
                _screen.update { it.copy(backupOngoing = false) }
            }
        }
    }

    private fun consumeBackupError() {
        _screen.update { it.copy(backupErrorMessage = null) }
    }
}
