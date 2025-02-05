package xyz.aprildown.timer.data.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.timer.data.mappers.BehaviourMapper
import xyz.aprildown.timer.data.mappers.SchedulerMapper
import xyz.aprildown.timer.data.mappers.StepMapper
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.data.mappers.TimerInfoMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.TimerMoreMapper
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.SchedulerRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository

class SchedulerRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = MachineDatabase.createInMemoryDatabase(context)
    private val timerRepository: TimerRepository = TimerRepositoryImpl(
        timerDao = database.timerDao(),
        timerMapper = TimerMapper(StepMapper(StepOnlyMapper(BehaviourMapper())), TimerMoreMapper()),
        timerInfoMapper = TimerInfoMapper(),
    )
    private var timerId: Int = TimerEntity.NULL_ID
    private val schedulerRepository: SchedulerRepository =
        SchedulerRepositoryImpl(database.schedulerDao(), SchedulerMapper())

    @Before
    fun setUp() = runTest {
        timerId = timerRepository.add(TestData.fakeTimerSimpleA)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun items_item_add_schedulersWithTimerId() = runTest {
        val list = arrayOf(
            TestData.fakeSchedulerA.copy(timerId = timerId),
            TestData.fakeSchedulerB.copy(timerId = timerId)
        )
        val ids = Array(list.size) { SchedulerEntity.NULL_ID }
        // At some at first
        list.forEachIndexed { index, schedulerEntity ->
            ids[index] = schedulerRepository.add(schedulerEntity)
        }

        // items, add, schedulersWithTimerId
        val result: List<SchedulerEntity> = schedulerRepository.items()
        assertEquals(2, result.size)
        val resultWithTimerId = schedulerRepository.schedulersWithTimerId(timerId)
        assertEquals(2, resultWithTimerId.size)
        result.forEachIndexed { index, schedulerEntity ->
            val expect = list[index].copy(id = ids[index])
            assertEquals(expect, schedulerEntity)
            assertEquals(expect, resultWithTimerId[index])
        }

        // item
        val item = schedulerRepository.item(ids[0])
        assertEquals(item, list[0])
    }

    @Test
    fun save() = runTest {
        val id = schedulerRepository.add(TestData.fakeSchedulerA.copy(timerId = timerId))

        val item = schedulerRepository.item(id)
        assertEquals(TestData.fakeSchedulerA.copy(id = id, timerId = timerId), item)

        val newData = TestData.fakeSchedulerB.copy(id = id, timerId = timerId)
        assertTrue(schedulerRepository.save(newData))
        val new = schedulerRepository.item(id)
        assertEquals(newData, new)
    }

    @Test
    fun delete() = runTest {
        val id = schedulerRepository.add(TestData.fakeSchedulerA.copy(timerId = timerId))

        val item = schedulerRepository.item(id)
        assertEquals(TestData.fakeSchedulerA.copy(id = id, timerId = timerId), item)

        schedulerRepository.delete(id)

        assertNull(schedulerRepository.item(id))
    }

    @Test
    fun setSchedulerEnable() = runTest {
        val id = schedulerRepository.add(
            TestData.fakeSchedulerA.copy(timerId = timerId, enable = 0)
        )

        val item = schedulerRepository.item(id)
        assertEquals(TestData.fakeSchedulerA.copy(id = id, timerId = timerId), item)

        schedulerRepository.setSchedulerEnable(id, 1)

        val newItem = schedulerRepository.item(id)
        assertEquals(TestData.fakeSchedulerA.copy(id = id, timerId = timerId, enable = 1), newItem)
    }
}
