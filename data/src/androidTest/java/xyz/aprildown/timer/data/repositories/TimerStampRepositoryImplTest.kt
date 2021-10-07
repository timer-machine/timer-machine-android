package xyz.aprildown.timer.data.repositories

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.timer.data.mappers.BehaviourMapper
import xyz.aprildown.timer.data.mappers.StepMapper
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.TimerMoreMapper
import xyz.aprildown.timer.data.mappers.TimerStampMapper
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.TimerStampEntity
import xyz.aprildown.timer.domain.repositories.TimerStampRepository

class TimerStampRepositoryImplTest {

    private val database = MachineDatabase.createInMemoryDatabase(
        ApplicationProvider.getApplicationContext()
    )
    private val repo: TimerStampRepository = TimerStampRepositoryImpl(
        database.timerStampDao(),
        TimerStampMapper()
    )
    private var timerId: Int = 0

    @Before
    fun setUp() = runBlocking {
        val timerMapper =
            TimerMapper(StepMapper(StepOnlyMapper(BehaviourMapper())), TimerMoreMapper())
        timerId = database.timerDao().addTimer(timerMapper.mapTo(TestData.fakeTimerSimpleA)).toInt()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun add_get_then_delete() = runBlocking {
        val stamp1 = TestData.fakeTimerStampA.copy(timerId = timerId)
        val stamp2 = TestData.fakeTimerStampB.copy(timerId = timerId)
        val stamps = listOf(stamp1, stamp2)
        repo.add(stamp1)
        repo.add(stamp2)
        val now = System.currentTimeMillis()
        assertEquals(stamps, repo.getAll())
        assertEquals(stamps, repo.getRaw(listOf(timerId), 0, now))
        assertEquals(2, repo.deleteWithTimerId(timerId))
        assertEquals(listOf<TimerStampEntity>(), repo.getAll())
        assertEquals(listOf<TimerStampEntity>(), repo.getRaw(listOf(timerId), 0, now))
    }
}