package xyz.aprildown.timer.domain.usecases.scheduler

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.testCoroutineDispatcher
import xyz.aprildown.timer.domain.usecases.invoke

class SchedulerUseCasesTest {

    private val schedulerRepository: SchedulerRepository = mock()
    private val schedulerExecutor: SchedulerExecutor = mock()
    private val appDataRepository: AppDataRepository = mock()

    @Test
    fun schedulers() = runBlocking {
        val schedulerList = listOf(TestData.fakeSchedulerA, TestData.fakeSchedulerB)
        whenever(schedulerRepository.items()).thenReturn(schedulerList)
        assertEquals(
            schedulerList,
            GetSchedulers(testCoroutineDispatcher, schedulerRepository).invoke()
        )
        verify(schedulerRepository).items()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun scheduler() = runBlocking {
        whenever(schedulerRepository.item(TestData.fakeSchedulerId)).thenReturn(TestData.fakeSchedulerA)
        val useCase = GetScheduler(testCoroutineDispatcher, schedulerRepository)

        assertNull(useCase(SchedulerEntity.NULL_ID))
        verify(schedulerRepository, never()).item(TestData.fakeSchedulerId)

        val result = useCase(TestData.fakeSchedulerId)
        assertEquals(TestData.fakeSchedulerA, result)
        verify(schedulerRepository).item(TestData.fakeSchedulerId)

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun add() = runBlocking {
        whenever(schedulerRepository.add(TestData.fakeSchedulerA))
            .thenReturn(TestData.fakeSchedulerId)
        val useCase = AddScheduler(testCoroutineDispatcher, schedulerRepository, appDataRepository)

        assertEquals(TestData.fakeSchedulerId, useCase(TestData.fakeSchedulerA))
        verify(schedulerRepository).add(TestData.fakeSchedulerA)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun save() = runBlocking {
        val old = TestData.fakeSchedulerA.copy(id = TestData.fakeSchedulerId)
        val new = TestData.fakeSchedulerB.copy(id = TestData.fakeSchedulerId)
        whenever(schedulerRepository.item(TestData.fakeSchedulerId)).thenReturn(old)
        whenever(schedulerRepository.save(new)).thenReturn(true)
        val useCase = SaveScheduler(
            testCoroutineDispatcher,
            schedulerRepository,
            schedulerExecutor,
            appDataRepository
        )

        assertEquals(
            false,
            useCase(new.copy(id = SchedulerEntity.NULL_ID))
        )
        verify(schedulerRepository, never()).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor, never()).cancel(old)
        verify(schedulerRepository, never()).add(new)
        verify(appDataRepository, never()).notifyDataChanged()

        assertEquals(true, useCase(new))
        verify(schedulerRepository).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor).cancel(old)
        verify(schedulerRepository).save(new)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun delete() = runBlocking {
        whenever(schedulerRepository.item(TestData.fakeSchedulerId)).thenReturn(TestData.fakeSchedulerA)
        val useCase = DeleteScheduler(
            testCoroutineDispatcher,
            schedulerRepository,
            schedulerExecutor,
            appDataRepository
        )

        useCase(SchedulerEntity.NULL_ID)
        verify(schedulerRepository, never()).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor, never()).cancel(TestData.fakeSchedulerA)
        verify(schedulerRepository, never()).delete(TestData.fakeSchedulerId)
        verify(appDataRepository, never()).notifyDataChanged()

        useCase(TestData.fakeSchedulerId)
        verify(schedulerRepository).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor).cancel(TestData.fakeSchedulerA)
        verify(schedulerRepository).delete(TestData.fakeSchedulerId)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun setSchedulerEnable() = runBlocking {
        val old = TestData.fakeSchedulerA.copy(enable = 0)
        whenever(schedulerRepository.item(TestData.fakeSchedulerId)).thenReturn(old)
        val useCase = SetSchedulerEnable(
            testCoroutineDispatcher,
            schedulerRepository,
            schedulerExecutor,
            appDataRepository
        )

        val new = old.copy(enable = 1)
        useCase(SetSchedulerEnable.Params(TestData.fakeSchedulerId, 1))
        verify(schedulerRepository).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor).schedule(new)
        verify(schedulerExecutor, never()).cancel(new)
        verify(schedulerRepository).setSchedulerEnable(TestData.fakeSchedulerId, 1)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun setSchedulerDisable() = runBlocking {
        val old = TestData.fakeSchedulerA.copy(enable = 1)
        whenever(schedulerRepository.item(TestData.fakeSchedulerId)).thenReturn(old)
        val useCase = SetSchedulerEnable(
            testCoroutineDispatcher,
            schedulerRepository,
            schedulerExecutor,
            appDataRepository
        )

        val new = old.copy(enable = 0)
        useCase(SetSchedulerEnable.Params(TestData.fakeSchedulerId, 0))
        verify(schedulerRepository).item(TestData.fakeSchedulerId)
        verify(schedulerExecutor).cancel(new)
        verify(schedulerExecutor, never()).schedule(new)
        verify(schedulerRepository).setSchedulerEnable(TestData.fakeSchedulerId, 0)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(schedulerRepository)
        verifyNoMoreInteractions(schedulerExecutor)
        verifyNoMoreInteractions(appDataRepository)
    }
}
