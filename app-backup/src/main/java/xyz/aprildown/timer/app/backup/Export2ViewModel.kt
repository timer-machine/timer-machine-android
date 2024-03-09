package xyz.aprildown.timer.app.backup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class Export2ViewModel @Inject constructor() : ViewModel() {
    @Immutable
    data class Screen(
        val saveLocation: String? = null,

        val includeTimers: Boolean = true,
        val onTimersChange: (Boolean) -> Unit,

        val includeRecords: Boolean = true,
        val onRecordsChange: (Boolean) -> Unit,

        val includeSchedulers: Boolean = true,
        val onSchedulersChange: (Boolean) -> Unit,

        val includeSettings: Boolean = true,
        val onSettingsChange: (Boolean) -> Unit,

        val onExport: () -> Unit,
        val exporting: Boolean = false,

        val exportError: Throwable? = null,
        val consumeExportError: () -> Unit,
    )

    private val _screen: MutableStateFlow<Screen> = MutableStateFlow(
        Screen(
            onTimersChange = ::onTimersChange,
            onRecordsChange = ::onRecordsChange,
            onSchedulersChange = ::onSchedulersChange,
            onSettingsChange = ::onSettingsChange,
            onExport = ::onExport,
            consumeExportError = ::consumeExportError,
        )
    )
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    fun setSaveLocation(location: String) {
        _screen.update { it.copy(saveLocation = location) }
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

    private fun onExport() {
        val screen = _screen.value
        if (screen.saveLocation.isNullOrBlank()) return
        if (!screen.includeTimers && !screen.includeSettings) return
        if (screen.exporting) return
        viewModelScope.launch {
            _screen.update { it.copy(exporting = true) }
            try {
                delay(5500)
                _screen.update { it.copy(exportError = Throwable("Hey")) }
            } finally {
                _screen.update { it.copy(exporting = false) }
            }
        }
    }

    private fun consumeExportError() {
        _screen.update { it.copy(exportError = null) }
    }
}
