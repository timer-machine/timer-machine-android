package xyz.aprildown.timer.presentation.one

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
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.SaveTimer
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getFirstIndex
import xyz.aprildown.timer.presentation.testCoroutineDispatcher

class OneViewModelTest {

    @JvmField
    @Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val timerRepository: TimerRepository = mock()
    private val intentProvider: StreamMachineIntentProvider = mock()
    private val appDataRepository: AppDataRepository = mock()

    private val timerObserver: Observer<TimerEntity> = mock()
    private val editTimerObserver: Observer<Event<Int>> = mock()
    private val serviceTriggerObserver: Observer<Event<Intent>> = mock()

    private val presenter: MachineContract.Presenter = mock()

    private lateinit var viewModel: OneViewModel

    @Before
    fun setUp() {
        viewModel = OneViewModel(
            testCoroutineDispatcher,
            GetTimer(testCoroutineDispatcher, timerRepository),
            SaveTimer(testCoroutineDispatcher, timerRepository, appDataRepository),
            FindTimerInfo(testCoroutineDispatcher, timerRepository),
            intentProvider,
            mock()
        )
        viewModel.timer.observeForever(timerObserver)
        viewModel.editTimerEvent.observeForever(editTimerObserver)
        viewModel.intentEvent.observeForever(serviceTriggerObserver)
    }

    @Test
    fun load() = runBlocking {
        val t = TestData.fakeTimerAdvanced
        whenever(timerRepository.item(t.id)).thenReturn(t)
        whenever(presenter.getTimerStateInfo(t.id)).thenReturn(null)
        viewModel.setTimerId(t.id)
        viewModel.setPresenter(presenter)

        verify(presenter).addListener(t.id, viewModel)

        viewModel.run {
            verify(timerRepository).item(t.id)
            verify(timerObserver).onChanged(t)

            assertEquals(t, timer.value)
            assertEquals(StreamState.RESET, timerCurrentState.value)
            assertEquals(t.getFirstIndex(), timerCurrentIndex.value)

            verifyNoInteractions(editTimerObserver)
            verifyNoInteractions(serviceTriggerObserver)
        }

        viewModel.dropPresenter()
        verify(presenter).removeListener(t.id, viewModel)
    }

    @Test
    fun actions() {
        val id = TestData.fakeTimerId
        viewModel.setTimerId(id)
        val startIntent = Intent()
        whenever(intentProvider.startIntent(eq(id), any())).thenReturn(startIntent)
        val adjustAmountIntent = Intent()
        whenever(intentProvider.adjustTimeIntent(eq(id), any())).thenReturn(adjustAmountIntent)
        val increIntent = Intent()
        whenever(intentProvider.increIntent(id)).thenReturn(increIntent)
        val decreIntent = Intent()
        whenever(intentProvider.decreIntent(id)).thenReturn(decreIntent)
        val pauseIntent = Intent()
        whenever(intentProvider.pauseIntent(id)).thenReturn(pauseIntent)
        val resetIntent = Intent()
        whenever(intentProvider.resetIntent(id)).thenReturn(resetIntent)

        viewModel.timerCurrentState.value = StreamState.RESET
        viewModel.timerCurrentIndex.value = TimerIndex.Start

        // From RESET to RUNNING
        viewModel.onStartPause()
        verify(intentProvider).startIntent(eq(id), any())
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
        }

        viewModel.started(0, TimerIndex.Start)
        assertEquals(StreamState.RUNNING, viewModel.timerCurrentState.value)

        // PlusOne
        viewModel.tweakTime(100000)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver, times(2)).onChanged(capture())
            assertEquals(2, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
            Assert.assertTrue(adjustAmountIntent == secondValue.peekContent())
        }

        // Incre
        viewModel.onMove(1)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver, times(3)).onChanged(capture())
            assertEquals(3, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
            Assert.assertTrue(adjustAmountIntent == secondValue.peekContent())
            Assert.assertTrue(increIntent == thirdValue.peekContent())
        }

        // Decre
        viewModel.onMove(-1)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver, times(4)).onChanged(capture())
            assertEquals(4, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
            Assert.assertTrue(adjustAmountIntent == secondValue.peekContent())
            Assert.assertTrue(increIntent == thirdValue.peekContent())
            Assert.assertTrue(decreIntent == allValues[3].peekContent())
        }

        // From RUNNING to PAUSED
        viewModel.onStartPause()
        verify(intentProvider).pauseIntent(id)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver, times(5)).onChanged(capture())
            assertEquals(5, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
            Assert.assertTrue(adjustAmountIntent == secondValue.peekContent())
            Assert.assertTrue(increIntent == thirdValue.peekContent())
            Assert.assertTrue(decreIntent == allValues[3].peekContent())
            Assert.assertTrue(pauseIntent == allValues[4].peekContent())
        }
        viewModel.paused(0)
        assertEquals(StreamState.PAUSED, viewModel.timerCurrentState.value)

        // From PAUSED to RESET
        viewModel.onReset()
        verify(intentProvider)
            .resetIntent(id)
        argumentCaptor<Event<Intent>> {
            verify(serviceTriggerObserver, times(6)).onChanged(capture())
            assertEquals(6, allValues.size)
            Assert.assertTrue(startIntent == firstValue.peekContent())
            Assert.assertTrue(adjustAmountIntent == secondValue.peekContent())
            Assert.assertTrue(increIntent == thirdValue.peekContent())
            Assert.assertTrue(decreIntent == allValues[3].peekContent())
            Assert.assertTrue(pauseIntent == allValues[4].peekContent())
            Assert.assertTrue(resetIntent == allValues[5].peekContent())
        }
        viewModel.end(0, true)
        assertEquals(StreamState.RESET, viewModel.timerCurrentState.value)
    }

    @Test
    fun action_edit() {
        val id = TestData.fakeTimerId
        viewModel.setTimerId(id)
        viewModel.onEdit()
        argumentCaptor<Event<Int>> {
            verify(editTimerObserver).onChanged(capture())
            assertEquals(1, allValues.size)
            Assert.assertTrue(id == firstValue.peekContent())
        }
    }

    @Test
    fun `action update start step`() = runBlocking {
        val t = TestData.fakeTimerAdvanced
        whenever(timerRepository.save(any())).thenReturn(true)
        val newStep = TestData.fakeStepB
        viewModel.timer.value = t

        viewModel.updateStep(TimerIndex.Start, newStep).join()

        val captor = argumentCaptor<TimerEntity>()
        verify(timerRepository).save(captor.capture())
        assertEquals(t.copy(startStep = newStep), captor.firstValue)
    }

    @Test
    fun `action update end step`() = runBlocking {
        val t = TestData.fakeTimerAdvanced
        whenever(timerRepository.save(any())).thenReturn(true)
        val newStep = TestData.fakeStepB
        viewModel.timer.value = t

        viewModel.updateStep(TimerIndex.End, newStep).join()

        val captor = argumentCaptor<TimerEntity>()
        verify(timerRepository).save(captor.capture())
        assertEquals(t.copy(endStep = newStep), captor.firstValue)
    }

    @Test
    fun `action update normal step`() = runBlocking {
        val t = TestData.fakeTimerAdvanced
        whenever(timerRepository.save(any())).thenReturn(true)
        val newStep = TestData.fakeStepB
        viewModel.timer.value = t

        viewModel.updateStep(TimerIndex.Step(3, 2), newStep).join()

        val captor = argumentCaptor<TimerEntity>()
        verify(timerRepository).save(captor.capture())
        assertEquals(
            t.copy(steps = t.steps.toMutableList().apply { set(2, newStep) }),
            captor.firstValue
        )
    }

    @Test
    fun `action update group step`() = runBlocking {
        val t = TestData.fakeTimerAdvanced
        whenever(timerRepository.save(any())).thenReturn(true)
        val newStep = TestData.fakeStepA
        viewModel.timer.value = t

        viewModel.updateStep(TimerIndex.Group(3, 1, TimerIndex.Step(0, 1)), newStep).join()

        val captor = argumentCaptor<TimerEntity>()
        verify(timerRepository).save(captor.capture())
        assertEquals(
            t.copy(
                steps = t.steps.toMutableList().apply {
                    set(
                        1,
                        TestData.fakeStepD.copy(
                            steps = listOf(
                                TestData.fakeStepA,
                                // This is changed from B to A
                                TestData.fakeStepA,
                                TestData.fakeStepC,
                            )
                        )
                    )
                }
            ),
            captor.firstValue
        )
    }
}
