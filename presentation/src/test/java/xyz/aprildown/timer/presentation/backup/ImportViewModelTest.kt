package xyz.aprildown.timer.presentation.backup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged

class ImportViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val importAppData: ImportAppData = mock()
    private val notifyDataChanged: NotifyDataChanged = mock()

    @Test
    fun import() = runTest {
        val viewModel =
            ImportViewModel(StandardTestDispatcher(testScheduler), importAppData, notifyDataChanged)

        val params = ImportAppData.Params(
            data = "",
            wipeFirst = false,
            importTimers = false,
            importTimerStamps = false,
            importSchedulers = false,
            importPreferences = false,
        )
        viewModel.import(params)

        testScheduler.advanceUntilIdle()

        verify(importAppData).invoke(params)
        verify(notifyDataChanged).invoke()

        verifyNoMoreInteractions(importAppData)
        verifyNoMoreInteractions(notifyDataChanged)
    }
}
