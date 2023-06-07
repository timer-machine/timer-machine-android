package xyz.aprildown.timer.presentation.scheduler

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.github.deweyreed.tools.arch.Event
import com.github.deweyreed.tools.helper.toInt
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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

class SchedulerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val getSchedulers: GetSchedulers = mock()
    private val findTimerInfo: FindTimerInfo = mock()
    private val setSchedulerEnable: SetSchedulerEnable = mock()
    private val deleteScheduler: DeleteScheduler = mock()

    private val scheduleObserver: Observer<Event<SetSchedulerEnable.Result>> = mock()
    private val schedulerWithTimerInfoObserver: Observer<List<Pair<SchedulerEntity, TimerInfo?>>> =
        mock()

    private fun TestScope.getViewModel(): SchedulerViewModel {
        val viewModel = SchedulerViewModel(
            mainDispatcher = StandardTestDispatcher(testScheduler),
            getSchedulers = getSchedulers,
            findTimerInfo = findTimerInfo,
            setSchedulerEnable = setSchedulerEnable,
            deleteScheduler = deleteScheduler,
        )
        viewModel.scheduleEvent.observeForever(scheduleObserver)
        viewModel.schedulerWithTimerInfo.observeForever(schedulerWithTimerInfoObserver)
        return viewModel
    }

    @Test
    fun load() = runTest {
        val viewModel = getViewModel()
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
    fun toggle() = runTest {
        val viewModel = getViewModel()
        val scheduler = TestData.fakeSchedulerA

        val result: SetSchedulerEnable.Result = SetSchedulerEnable.Result.Scheduled(0L)
        whenever(setSchedulerEnable(any())).thenReturn(result)

        viewModel.toggleSchedulerState(scheduler.id, true)

        testScheduler.advanceUntilIdle()

        argumentCaptor<Event<SetSchedulerEnable.Result>> {
            verify(scheduleObserver).onChanged(capture())
            Assert.assertEquals(1, allValues.size)
            Assert.assertTrue(result == firstValue.peekContent())
        }
        verify(setSchedulerEnable).invoke(SetSchedulerEnable.Params(scheduler.id, true.toInt()))

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun delete() = runTest {
        val viewModel = getViewModel()
        val s = TestData.fakeSchedulerB
        whenever(deleteScheduler(any())).thenReturn(Unit)
        viewModel.delete(s.id)
        testScheduler.advanceUntilIdle()
        verify(deleteScheduler).invoke(s.id)
        verifyNoMoreInteractionsForAll()
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(getSchedulers)
        verifyNoMoreInteractions(findTimerInfo)
        verifyNoMoreInteractions(setSchedulerEnable)
    }
}
