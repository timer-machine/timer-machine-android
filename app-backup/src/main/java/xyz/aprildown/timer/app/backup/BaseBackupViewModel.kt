package xyz.aprildown.timer.app.backup

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.presentation.BaseViewModel

internal abstract class BaseBackupViewModel<ContentType : BaseBackupViewModel.Screen.Content>(
    mainDispatcher: CoroutineDispatcher,
) : BaseViewModel(mainDispatcher) {
    @Immutable
    data class Screen<ContentType : Screen.Content>(
        val content: ContentType? = null,
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

        val backupResult: Fruit<Unit>? = null,
        val consumeBackupError: () -> Unit,
    ) {
        interface Content
    }

    private val _screen: MutableStateFlow<Screen<ContentType>> = MutableStateFlow(
        Screen(
            onTimersChange = ::onTimersChange,
            onRecordsChange = ::onRecordsChange,
            onSchedulersChange = ::onSchedulersChange,
            onSettingsChange = ::onSettingsChange,
            onBackup = ::onBackup,
            consumeBackupError = ::consumeBackupError,
        )
    )
    val screen: StateFlow<Screen<ContentType>> = _screen.asStateFlow()

    fun changeContent(content: ContentType, name: String?) {
        _screen.update { it.copy(content = content, contentName = name) }
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

    protected abstract suspend fun backup(screen: Screen<ContentType>)

    private fun onBackup() {
        val screen = _screen.value
        if (screen.content == null) return
        if (!screen.includeTimers && !screen.includeSettings) return
        if (screen.backupOngoing) return

        launch {
            _screen.update { it.copy(backupOngoing = true) }
            try {
                backup(screen)
                _screen.update {
                    it.copy(
                        backupOngoing = false,
                        backupResult = Fruit.Ripe(Unit),
                    )
                }
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                _screen.update {
                    it.copy(
                        backupOngoing = false,
                        backupResult = Fruit.Rotten(e),
                    )
                }
            }
        }
    }

    private fun consumeBackupError() {
        _screen.update { it.copy(backupResult = null) }
    }
}
