package xyz.aprildown.timer.presentation.screen

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.github.deweyreed.tools.arch.Event
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getNiceLoopString
import xyz.aprildown.timer.presentation.stream.getStep

class ScreenViewModelTest {

    @JvmField
    @Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val intentProvider: StreamMachineIntentProvider = mock()
    private val stopTimerObserver: Observer<Event<Unit>> = mock()
    private val serviceTriggerObserver: Observer<Event<Intent>> = mock()
    private val presenter: MachineContract.Presenter = mock()

    private lateinit var viewModel: ScreenViewModel

    @Before
    fun setUp() {
        viewModel = ScreenViewModel(intentProvider)
        viewModel.stopEvent.observeForever(stopTimerObserver)
        viewModel.intentEvent.observeForever(serviceTriggerObserver)
    }

    @Test
    fun load() {
        val t = TestData.fakeTimerSimpleB
        val currentIndex = TimerIndex.Step(loopIndex = 1, stepIndex = 0)
        whenever(presenter.getTimerStateInfo(t.id))
            .thenReturn(
                MachineContract.CurrentTimerInfo(t, StreamState.RUNNING, currentIndex, 10_000)
            )
        viewModel.setTimerId(t.id)
        viewModel.setPresenter(presenter)

        verify(presenter).addListener(t.id, viewModel)

        assertEquals(
            ScreenViewModel.formatStepInfo(
                timerName = t.name,
                loopString = currentIndex.getNiceLoopString(max = t.loop),
                stepName = t.getStep(currentIndex)?.label.toString()
            ),
            viewModel.timerStepInfo.value
        )

        assertEquals(10_000L, viewModel.timerCurrentTime.value)

        viewModel.dropPresenter()
        verify(presenter).removeListener(t.id, viewModel)
    }

    @Test
    fun stop() = runBlocking {
        val id = TestData.fakeTimerId
        val increIntent = Intent()
        whenever(intentProvider.increIntent(id)).thenReturn(increIntent)
        viewModel.setTimerId(id)
        viewModel.onStop()
        verify(intentProvider).increIntent(id)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            Assert.assertTrue(increIntent == firstValue.peekContent())
        }
        argumentCaptor<Event<Unit>> {
            verify(stopTimerObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            Assert.assertTrue(Unit == firstValue.peekContent())
        }
        verifyNoMoreInteractions(serviceTriggerObserver)
        verifyNoMoreInteractions(stopTimerObserver)
    }

    @Test
    fun plusOneMinute() = runBlocking {
        val id = TestData.fakeTimerId
        val time = 60_000L
        val addTimeIntent = Intent()
        whenever(intentProvider.adjustTimeIntent(id, time)).thenReturn(addTimeIntent)
        viewModel.setTimerId(id)
        viewModel.onAddOneMinute()
        verify(intentProvider).adjustTimeIntent(id, time)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            Assert.assertTrue(addTimeIntent == firstValue.peekContent())
        }
        verifyNoInteractions(stopTimerObserver)
        verifyNoMoreInteractions(intentProvider)
        verifyNoMoreInteractions(serviceTriggerObserver)
    }
}
