package xyz.aprildown.timer.app.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal abstract class BaseBackupViewModelTest {

    internal abstract fun TestScope.createViewModel(): BaseBackupViewModel<*>

    protected fun randomString(): String = System.currentTimeMillis().toString()

    @Test
    fun `check initial state`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onTimersChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onTimersChange(false)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertFalse(viewModel.screen.value.includeTimers)
        assertFalse(viewModel.screen.value.includeRecords)
        assertFalse(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onTimersToggle`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onTimersChange(false)
        viewModel.screen.value.onTimersChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onSchedulersChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onSchedulersChange(false)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertFalse(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onSchedulersToggle`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onSchedulersChange(false)
        viewModel.screen.value.onSchedulersChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onTimersSchedulersChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onTimersChange(false)
        viewModel.screen.value.onSchedulersChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertFalse(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onRecordsChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onRecordsChange(false)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertFalse(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onRecordsToggle`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onRecordsChange(false)
        viewModel.screen.value.onRecordsChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onTimersRecordsChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onTimersChange(false)
        viewModel.screen.value.onRecordsChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertFalse(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onSettingsChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onSettingsChange(false)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertFalse(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `check onSettingsToggle`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onSettingsChange(false)
        viewModel.screen.value.onSettingsChange(true)
        assertNull(viewModel.screen.value.content)
        assertNull(viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    /**
     * Will create new [ViewModelStore], add view model into it using [ViewModelProvider]
     * and then call [ViewModelStore.clear], that will cause [ViewModel.onCleared] to be called
     * https://stackoverflow.com/a/57052682/5507158
     */
    protected fun ViewModel.callOnCleared() {
        val viewModelStore = ViewModelStore()
        val viewModelProvider = ViewModelProvider(
            viewModelStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return this@callOnCleared as T
                }
            }
        )
        viewModelProvider[this@callOnCleared::class.java]
        viewModelStore.clear()
    }
}
