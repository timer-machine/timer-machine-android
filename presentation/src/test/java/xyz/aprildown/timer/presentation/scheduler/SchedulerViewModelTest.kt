package xyz.aprildown.timer.presentation.scheduler

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.domain.usecases.scheduler.DeleteScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.GetSchedulers
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.testCoroutineDispatcher
import xyz.aprildown.tools.arch.Event
import xyz.aprildown.tools.helper.toInt

class SchedulerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val getSchedulers: GetSchedulers = mock()
    private val findTimerInfo: FindTimerInfo = mock()
    private val setSchedulerEnable: SetSchedulerEnable = mock()
    private val deleteScheduler: DeleteScheduler = mock()

    private lateinit var viewModel: SchedulerViewModel

    private val scheduleObserver: Observer<Event<SetSchedulerEnable.Result>> = mock()
    private val schedulerWithTimerInfoObserver: Observer<List<Pair<SchedulerEntity, TimerInfo?>>> =
        mock()

    @Before
    fun setUp() {
        viewModel = SchedulerViewModel(
            mainDispatcher = testCoroutineDispatcher,
            getSchedulers = getSchedulers,
            findTimerInfo = findTimerInfo,
            setSchedulerEnable = setSchedulerEnable,
            deleteScheduler = deleteScheduler,
        )
        viewModel.scheduleEvent.observeForever(scheduleObserver)
        viewModel.schedulerWithTimerInfo.observeForever(schedulerWithTimerInfoObserver)
    }

    @Test
    fun load() = runBlocking {
        val schedulers = listOf(TestData.fakeSchedulerA, TestData.fakeSchedulerB)
        val timerInfo = TestData.fakeTimerSimpleA.toTimerInfo()
        whenever(getSchedulers()).thenReturn(schedulers)
        schedulers.forEach {
            whenever(findTimerInfo(it.timerId)).thenReturn(timerInfo)
        }

        viewModel.load().join()

        verify(getSchedulers).invoke()
        verify(findTimerInfo, atLeast(1)).invoke(any())
        verify(schedulerWithTimerInfoObserver).onChanged(schedulers.map { it to timerInfo })

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun toggle() = runBlocking {
        val scheduler = TestData.fakeSchedulerA

        val result: SetSchedulerEnable.Result = mock()
        whenever(setSchedulerEnable(any())).thenReturn(result)

        viewModel.toggleSchedulerState(scheduler.id, true)

        argumentCaptor<Event<SetSchedulerEnable.Result>> {
            verify(scheduleObserver).onChanged(capture())
            Assert.assertEquals(1, allValues.size)
            Assert.assertTrue(result == firstValue.peekContent())
        }
        verify(setSchedulerEnable).invoke(SetSchedulerEnable.Params(scheduler.id, true.toInt()))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun delete() = runBlocking {
        val s = TestData.fakeSchedulerB
        whenever(deleteScheduler(any())).thenReturn(Unit)
        viewModel.delete(s.id)
        verify(deleteScheduler).invoke(s.id)
        verifyNoMoreInteractionsForAll()
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(getSchedulers)
        verifyNoMoreInteractions(findTimerInfo)
        verifyNoMoreInteractions(setSchedulerEnable)
    }
}
