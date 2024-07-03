package xyz.aprildown.timer.backup

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import xyz.aprildown.timer.app.backup.ImportViewModel
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged

internal class ImportViewModelTest : BaseBackupViewModelTest() {
    private val importAppData: ImportAppData = mock()
    private val notifyDataChanged: NotifyDataChanged = mock()

    override fun TestScope.createViewModel(): ImportViewModel {
        return ImportViewModel(
            mainDispatcher = StandardTestDispatcher(testScheduler),
            importAppData = importAppData,
            notifyDataChanged = notifyDataChanged,
        )
    }

    @Test
    fun `check import initial state`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.importScreen.value.wipe)
    }

    @Test
    fun `check onWipeChange`() = runTest {
        val viewModel = createViewModel()
        viewModel.importScreen.value.onWipeChange(true)
        assertTrue(viewModel.importScreen.value.wipe)
    }

    @Test
    fun `check changeContent`() = runTest {
        val viewModel = createViewModel()

        val content = ImportViewModel.ReadableContent(getSource = { mock() })
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
    fun backUp() = runTest {
        val viewModel = createViewModel()
        val buffer = Buffer()
        val data = randomString()
        buffer.writeUtf8(data)
        viewModel.changeContent(
            content = ImportViewModel.ReadableContent(getSource = { buffer }),
            name = randomString(),
        )
        viewModel.screen.value.onBackup()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.screen.value.backupResult is Fruit.Ripe<*>)
        verify(importAppData).invoke(
            ImportAppData.Params(
                data = data,
                wipeFirst = viewModel.importScreen.value.wipe,
                importTimers = viewModel.screen.value.includeTimers,
                importTimerStamps = viewModel.screen.value.includeRecords,
                importSchedulers = viewModel.screen.value.includeSchedulers,
                importPreferences = viewModel.screen.value.includeSettings,
            )
        )
        verifyNoMoreInteractions(importAppData)
        verify(notifyDataChanged).invoke()
        verifyNoMoreInteractions(notifyDataChanged)
    }

    @After
    fun after() {
        verifyNoMoreInteractions(importAppData)
        verifyNoMoreInteractions(notifyDataChanged)
    }
}
