package xyz.aprildown.timer.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.entities.AppDataEntity
import xyz.aprildown.timer.domain.entities.FolderSortBy
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.toTimerInfo
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.FolderRepository
import xyz.aprildown.timer.domain.repositories.SchedulerExecutor
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.repositories.TimerStampRepository
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.ChangeTimerFolder
import xyz.aprildown.timer.domain.usecases.timer.DeleteTimer
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.domain.usecases.timer.GetTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.SaveTimer
import xyz.aprildown.timer.domain.usecases.timer.ShareTimer
import kotlin.random.Random

class TimerUseCasesTest {

    private val folderRepository: FolderRepository = mock()
    private val timerRepository: TimerRepository = mock()
    private val appDataRepository: AppDataRepository = mock()

    @Test
    fun timer() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        whenever(timerRepository.item(TestData.fakeTimerId)).thenReturn(timer)
        val useCase = GetTimer(testCoroutineDispatcher, timerRepository)

        assertNull(useCase.execute(TimerEntity.NULL_ID))
        verify(timerRepository, never()).item(TestData.fakeTimerId)

        val result = useCase.execute(TestData.fakeTimerId)
        assertEquals(timer, result)
        verify(timerRepository).item(TestData.fakeTimerId)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun add() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        whenever(timerRepository.add(timer)).thenReturn(TestData.fakeTimerId)
        val useCase = AddTimer(testCoroutineDispatcher, timerRepository, appDataRepository)

        assertEquals(timer.id, useCase.execute(timer))
        verify(timerRepository).add(timer)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun save() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        whenever(timerRepository.save(timer)).thenReturn(true)
        val useCase = SaveTimer(testCoroutineDispatcher, timerRepository, appDataRepository)

        assertEquals(false, useCase.execute(timer.copy(id = TimerEntity.NULL_ID)))
        verify(timerRepository, never()).save(any())

        assertEquals(true, useCase.execute(timer))
        verify(timerRepository).save(timer)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun delete() = runBlocking {
        val schedulerRepository = mock<SchedulerRepository>()
        val schedulerExecutor = mock<SchedulerExecutor>()
        val timerStampRepository = mock<TimerStampRepository>()
        val useCase = DeleteTimer(
            testCoroutineDispatcher,
            timerRepository,
            schedulerRepository,
            schedulerExecutor,
            timerStampRepository,
            appDataRepository
        )

        val timerId = TestData.fakeTimerId

        useCase.execute(TimerEntity.NULL_ID)
        verify(timerRepository, never()).delete(timerId)
        verify(appDataRepository, never()).notifyDataChanged()

        val schedulers = listOf(TestData.fakeSchedulerA, TestData.fakeSchedulerB)
        whenever(schedulerRepository.schedulersWithTimerId(timerId))
            .thenReturn(schedulers)
        whenever(timerStampRepository.deleteWithTimerId(timerId)).thenReturn(any())
        useCase.execute(timerId)
        verify(schedulerRepository).schedulersWithTimerId(timerId)
        verify(schedulerExecutor).cancel(TestData.fakeSchedulerA)
        verify(schedulerExecutor).cancel(TestData.fakeSchedulerB)
        verify(schedulerRepository).delete(TestData.fakeSchedulerA.id)
        verify(schedulerRepository).delete(TestData.fakeSchedulerB.id)
        verify(timerStampRepository).deleteWithTimerId(timerId)
        verify(timerRepository).delete(TestData.fakeTimerId)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `change timer folder`() = runBlocking {
        val useCase = ChangeTimerFolder(testCoroutineDispatcher, timerRepository, appDataRepository)
        useCase.execute(ChangeTimerFolder.Params(0, 0))
        verify(timerRepository).changeTimerFolder(0, 0)
        verify(appDataRepository).notifyDataChanged()

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `find null timer info`() = runBlocking {
        val useCase = FindTimerInfo(testCoroutineDispatcher, timerRepository)

        assertEquals(null, useCase.execute(TimerEntity.NULL_ID))

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `find timer info`() = runBlocking {
        val timer = TestData.fakeTimerSimpleA
        val timerId = timer.id
        whenever(timerRepository.getTimerInfoByTimerId(timerId)).thenReturn(timer.toTimerInfo())

        val useCase = FindTimerInfo(testCoroutineDispatcher, timerRepository)

        assertEquals(timer.toTimerInfo(), useCase.execute(timerId))
        verify(timerRepository).getTimerInfoByTimerId(timerId)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `get timer info without stamps`() = runBlocking {
        val timers = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )
        val timerInfo = timers.map { it.toTimerInfo() }
        val folderId = TestData.fakeFolderId

        whenever(timerRepository.getTimerInfo(folderId)).thenReturn(timerInfo)

        val useCase = GetTimerInfo(
            dispatcher = testCoroutineDispatcher,
            repository = timerRepository,
            timerStampRepository = mock()
        )

        val result1 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.AddedNewest))
        assertEquals(timerInfo.sortedByDescending { it.id }, result1)

        val result2 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.AddedOldest))
        assertEquals(timerInfo.sortedBy { it.id }, result2)

        val result3 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.AToZ))
        assertEquals(timerInfo.sortedBy { it.name }, result3)

        val result4 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.ZToA))
        assertEquals(timerInfo.sortedByDescending { it.name }, result4)

        verify(timerRepository, times(4)).getTimerInfo(folderId)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `get timer info with stamps`() = runBlocking {
        val timers = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )
        val timerInfo = timers.map { it.toTimerInfo() }
        val folderId = TestData.fakeFolderId

        whenever(timerRepository.getTimerInfo(folderId)).thenReturn(timerInfo)

        val timerStampRepo: TimerStampRepository = mock()

        val timerIdWithEndTimeMap = mutableMapOf<Int, Long>()
        timers.map { it.id }.distinct().forEach { timerId ->
            val end = Random.nextLong()
            val timerStamp = TestData.fakeTimerStampA.copy(end = end)
            timerIdWithEndTimeMap[timerId] = end
            whenever(timerStampRepo.getRecentOne(timerId)).thenReturn(timerStamp)
        }

        val useCase = GetTimerInfo(
            dispatcher = testCoroutineDispatcher,
            repository = timerRepository,
            timerStampRepository = { timerStampRepo }
        )

        val result1 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.RunNewest))
        assertEquals(timerInfo.sortedByDescending { timerIdWithEndTimeMap[it.id] }, result1)

        val result2 = useCase(GetTimerInfo.Params(folderId, FolderSortBy.RunOldest))
        assertEquals(timerInfo.sortedBy { timerIdWithEndTimeMap[it.id] }, result2)

        verify(timerRepository, times(2)).getTimerInfo(folderId)
        verify(timerStampRepo, atLeast(timers.size * 2)).getRecentOne(any())

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(timerStampRepo)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `share timer test`() = runBlocking {
        val timers = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )

        val string = "Test result"
        whenever(appDataRepository.collectData(AppDataEntity(timers = timers))).thenReturn(string)

        val useCase = ShareTimer(
            dispatcher = testCoroutineDispatcher,
            appDataRepository = appDataRepository,
        )

        val fruit = useCase.shareAsString(timers)
        assertTrue(fruit is Fruit.Ripe && fruit.data === string)

        verify(appDataRepository).collectData(AppDataEntity(timers = timers))

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }

    @Test
    fun `receive shared timer test`() = runBlocking {
        val timers = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )

        val string = "Test result"
        whenever(appDataRepository.unParcelData(string)).thenReturn(
            AppDataEntity(
                folders = listOf(TestData.defaultFolder),
                timers = timers,
                notifier = TestData.fakeStepA,
                timerStamps = listOf(TestData.fakeTimerStampA, TestData.fakeTimerStampB),
                schedulers = listOf(TestData.fakeSchedulerA, TestData.fakeSchedulerB),
            )
        )

        val useCase = ShareTimer(
            dispatcher = testCoroutineDispatcher,
            appDataRepository = appDataRepository,
        )

        val fruit = useCase.receiveFromString(string)

        assertTrue(fruit is Fruit.Ripe && fruit.data == AppDataEntity(timers = timers))

        verify(appDataRepository).unParcelData(string)

        verifyNoMoreInteractions(folderRepository)
        verifyNoMoreInteractions(timerRepository)
        verifyNoMoreInteractions(appDataRepository)
    }
}
