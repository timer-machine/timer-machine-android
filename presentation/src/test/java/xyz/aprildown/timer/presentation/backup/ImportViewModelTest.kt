package xyz.aprildown.timer.presentation.backup

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged
import xyz.aprildown.timer.presentation.testCoroutineDispatcher

class ImportViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val importAppData: ImportAppData = mock()
    private val notifyDataChanged: NotifyDataChanged = mock()

    private lateinit var viewModel: ImportViewModel

    @Before
    fun setUp() {
        viewModel =
            ImportViewModel(testCoroutineDispatcher, importAppData, notifyDataChanged)
    }

    @Test
    fun import() = runBlocking {
        whenever(importAppData.invoke(any())).thenReturn(emptyMap())
        viewModel.importAppData(
            ImportAppData.Params(
                data = "",
                wipeFirst = false,
                importTimers = false,
                importTimerStamps = false,
                importSchedulers = false
            ),
            handlePrefs = {
                assertEquals(emptyMap<String, String>(), it)
            }
        ) {}.join()

        verify(importAppData).invoke(any())
        verify(notifyDataChanged).invoke()

        verifyNoMoreInteractions(importAppData)
        verifyNoMoreInteractions(notifyDataChanged)
    }
}
