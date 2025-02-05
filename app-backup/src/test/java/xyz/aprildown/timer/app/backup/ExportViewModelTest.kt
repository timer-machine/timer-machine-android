package xyz.aprildown.timer.app.backup

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ExportAppData

internal class ExportViewModelTest : BaseBackupViewModelTest() {
    private val exportAppData: ExportAppData = mock()

    override fun TestScope.createViewModel(): ExportViewModel {
        return ExportViewModel(
            mainDispatcher = StandardTestDispatcher(testScheduler),
            savedStateHandle = SavedStateHandle(),
            exportAppData = exportAppData,
        )
    }

    @Test
    fun changeContent() = runTest {
        val viewModel = createViewModel()

        val content = ExportViewModel.WritableContent(getSink = { error("error") }, delete = {})
        val name = randomString()
        viewModel.changeContent(content, name)

        assertSame(content, viewModel.screen.value.content)
        assertSame(name, viewModel.screen.value.contentName)
        assertTrue(viewModel.screen.value.includeTimers)
        assertTrue(viewModel.screen.value.includeRecords)
        assertTrue(viewModel.screen.value.includeSchedulers)
        assertTrue(viewModel.screen.value.includeSettings)
        assertFalse(viewModel.screen.value.backupOngoing)
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `backUp without content`() = runTest {
        val viewModel = createViewModel()
        viewModel.screen.value.onBackup()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun `backUp without options`() = runTest {
        val viewModel = createViewModel()
        viewModel.changeContent(
            content = ExportViewModel.WritableContent(getSink = { error("error") }, delete = {}),
            name = randomString(),
        )
        viewModel.screen.value.onTimersChange(false)
        viewModel.screen.value.onSettingsChange(false)
        viewModel.screen.value.onBackup()
        testScheduler.advanceUntilIdle()
        assertNull(viewModel.screen.value.backupResult)
    }

    @Test
    fun backUp() = runTest {
        val viewModel = createViewModel()
        val data = randomString()
        whenever(exportAppData.invoke(any())).thenReturn(data)
        val buffer = Buffer()
        viewModel.changeContent(
            content = ExportViewModel.WritableContent(getSink = { buffer }, delete = {}),
            name = randomString(),
        )
        viewModel.screen.value.onBackup()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.screen.value.backupResult is Fruit.Ripe<*>)
        assertEquals(data, buffer.readUtf8())

        verify(exportAppData).invoke(
            ExportAppData.Params(
                exportTimers = viewModel.screen.value.includeTimers,
                exportTimerStamps = viewModel.screen.value.includeRecords,
                exportSchedulers = viewModel.screen.value.includeSchedulers,
                exportPreferences = viewModel.screen.value.includeSettings,
            )
        )
        verifyNoMoreInteractions(exportAppData)
    }

    @Test
    fun onCleared() = runTest {
        val viewModel = createViewModel()

        var isDeleted = false
        val content = ExportViewModel.WritableContent(
            getSink = { error("error") },
            delete = { isDeleted = true }
        )
        viewModel.changeContent(content, "")
        viewModel.callOnCleared()
        assertTrue(isDeleted)
    }

    @After
    fun after() {
        verifyNoMoreInteractions(exportAppData)
    }
}
