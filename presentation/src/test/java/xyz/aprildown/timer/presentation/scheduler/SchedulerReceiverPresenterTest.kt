package xyz.aprildown.timer.presentation.scheduler

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.SchedulerRepeatMode
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.scheduler.GetScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.SetSchedulerEnable
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.testCoroutineDispatcher

class SchedulerReceiverPresenterTest {

    private val timerRepository: TimerRepository = mock()
    private val schedulerRepository: SchedulerRepository = mock()
    private val schedulerExecutor: SchedulerExecutor = mock()
    private val appDataRepository: AppDataRepository = mock()

    private lateinit var presenter: SchedulerReceiverPresenter

    @Before
    fun setUp() {
        presenter = SchedulerReceiverPresenter(
            FindTimerInfo(testCoroutineDispatcher, timerRepository),
            GetScheduler(testCoroutineDispatcher, schedulerRepository),
            SetSchedulerEnable(
                testCoroutineDispatcher,
                schedulerRepository,
                schedulerExecutor,
                appDataRepository
            )
        )
    }

    @Test
    fun handleFiredScheduler_once_and_no_more() = runBlocking<Unit> {
        val scheduler = TestData.fakeSchedulerA.copy(repeatMode = SchedulerRepeatMode.ONCE)
        whenever(schedulerRepository.item(scheduler.id)).thenReturn(scheduler)
        val result = presenter.handleFiredScheduler(scheduler.id)
        assertEquals(scheduler.timerId, result)
        verify(schedulerRepository).setSchedulerEnable(scheduler.id, 0)
        verify(schedulerExecutor, never()).schedule(any())
        verify(schedulerExecutor).cancel(scheduler.copy(enable = 0))
    }

    @Test
    fun handleFiredScheduler_repeat() = runBlocking<Unit> {
        val scheduler = TestData.fakeSchedulerA.copy(repeatMode = SchedulerRepeatMode.EVERY_DAYS)
        whenever(schedulerRepository.item(scheduler.id)).thenReturn(scheduler)
        val result = presenter.handleFiredScheduler(scheduler.id)
        assertEquals(scheduler.timerId, result)
        verify(schedulerRepository).setSchedulerEnable(scheduler.id, 1)
        verify(schedulerExecutor).schedule(scheduler.copy(enable = 1))
        verify(schedulerExecutor, never()).cancel(any())
    }
}
