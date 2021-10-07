package xyz.aprildown.timer.presentation.scheduler

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.usecases.scheduler.AddScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.GetScheduler
import xyz.aprildown.timer.domain.usecases.scheduler.SaveScheduler
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.testCoroutineDispatcher

class EditSchedulerViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val findTimerInfo: FindTimerInfo = mock()
    private val getScheduler: GetScheduler = mock()
    private val addScheduler: AddScheduler = mock()
    private val saveScheduler: SaveScheduler = mock()

    private lateinit var viewModel: EditSchedulerViewModel

    private val schedulerWithTimerInfoObserver: Observer<Pair<SchedulerEntity, TimerInfo?>> = mock()

    @Before
    fun setUp() {
        viewModel = EditSchedulerViewModel(
            mainDispatcher = testCoroutineDispatcher,
            findTimerInfo = findTimerInfo,
            getScheduler = getScheduler,
            addScheduler = addScheduler,
            saveSchedulerUseCase = saveScheduler
        )
        viewModel.schedulerWithTimerInfo.observeForever(schedulerWithTimerInfoObserver)
    }

    @Test
    fun load() = runBlocking {
        val scheduler = TestData.fakeSchedulerA
        whenever(getScheduler(scheduler.id)).thenReturn(scheduler)
        val timer = TestData.fakeTimerSimpleA
        val timerInfo = timer.toTimerInfo()
        whenever(findTimerInfo(scheduler.timerId)).thenReturn(timerInfo)

        viewModel.load(scheduler.id).join()
        viewModel.load(scheduler.id).join()

        verify(getScheduler).invoke(scheduler.id)
        verify(findTimerInfo).invoke(scheduler.timerId)
        verify(schedulerWithTimerInfoObserver).onChanged(scheduler to timerInfo)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `load new scheduler`() = runBlocking {
        viewModel.load(SchedulerEntity.NEW_ID).join()
        argumentCaptor<Pair<SchedulerEntity, TimerInfo?>> {
            verify(schedulerWithTimerInfoObserver).onChanged(capture())
            assertTrue(firstValue.first.isNull)
            assertNull(firstValue.second)
        }

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `save new scheduler`() = runBlocking {
        val new = TestData.fakeSchedulerB.copy(id = SchedulerEntity.NEW_ID)
        viewModel.load(SchedulerEntity.NEW_ID).join()
        viewModel.saveScheduler(new).join()

        argumentCaptor<Pair<SchedulerEntity, TimerInfo?>> {
            verify(schedulerWithTimerInfoObserver).onChanged(capture())
            assertTrue(firstValue.first.isNull)
            assertNull(firstValue.second)
        }
        verify(addScheduler).invoke(new)

        verifyNoMoreInteractionsForAll()
    }

    @Test
    fun `save scheduler`() = runBlocking {
        val old = TestData.fakeSchedulerA.copy(enable = 0)
        val new = TestData.fakeSchedulerB.copy(id = old.id, enable = 0)
        val timerInfo = TestData.fakeTimerAdvanced.toTimerInfo()
        whenever(getScheduler.invoke(old.id)).thenReturn(old)
        whenever(findTimerInfo.invoke(old.timerId)).thenReturn(timerInfo)

        viewModel.load(old.id).join()

        verify(getScheduler).invoke(old.id)
        verify(findTimerInfo).invoke(old.timerId)
        verify(schedulerWithTimerInfoObserver).onChanged(old to timerInfo)

        viewModel.saveScheduler(new).join()

        verify(saveScheduler).invoke(new)

        verifyNoMoreInteractionsForAll()
    }

    private fun verifyNoMoreInteractionsForAll() {
        verifyNoMoreInteractions(findTimerInfo)
        verifyNoMoreInteractions(getScheduler)
        verifyNoMoreInteractions(addScheduler)
        verifyNoMoreInteractions(saveScheduler)
        verifyNoMoreInteractions(schedulerWithTimerInfoObserver)
    }
}
