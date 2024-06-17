package xyz.aprildown.timer.data.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.aprildown.timer.data.db.MachineDatabase
import xyz.aprildown.timer.data.mappers.BehaviourMapper
import xyz.aprildown.timer.data.mappers.StepMapper
import xyz.aprildown.timer.data.mappers.StepOnlyMapper
import xyz.aprildown.timer.data.mappers.TimerInfoMapper
import xyz.aprildown.timer.data.mappers.TimerMapper
import xyz.aprildown.timer.data.mappers.TimerMoreMapper
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.TimerRepository

class TimerRepositoryImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = MachineDatabase.createInMemoryDatabase(context)
    private val timerRepository: TimerRepository = TimerRepositoryImpl(
        context = context,
        timerDao = database.timerDao(),
        timerMapper = TimerMapper(StepMapper(StepOnlyMapper(BehaviourMapper())), TimerMoreMapper()),
        timerInfoMapper = TimerInfoMapper(),
    )

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun items_item_add_idAndNames() = runTest {
        val list = listOf(
            TestData.fakeTimerSimpleA,
            TestData.fakeTimerSimpleB,
            TestData.fakeTimerAdvanced
        )
        val ids = Array(list.size) { TimerEntity.NULL_ID }
        // At some at first
        list.forEachIndexed { index, timerEntity ->
            ids[index] = timerRepository.add(timerEntity)
        }

        // items and add
        val result: List<TimerEntity> = timerRepository.items()
        assertEquals(3, result.size)
        result.forEachIndexed { index, timerEntity ->
            assertEquals(list[index].copy(id = ids[index]), timerEntity)
        }

        // item
        val item = timerRepository.item(ids[0])
        assertEquals(list[0], item)

        assertEquals(list, timerRepository.items())
    }

    @Test
    fun save() = runTest {
        val id = timerRepository.add(TestData.fakeTimerSimpleA)

        val item = timerRepository.item(id)
        assertEquals(TestData.fakeTimerSimpleA.copy(id = id), item)

        assertTrue(timerRepository.save(TestData.fakeTimerAdvanced.copy(id = id)))
        val new = timerRepository.item(id)
        assertEquals(TestData.fakeTimerAdvanced.copy(id = id), new)
    }

    @Test
    fun delete() = runTest {
        val id = timerRepository.add(TestData.fakeTimerSimpleA)

        val item = timerRepository.item(id)
        assertEquals(TestData.fakeTimerSimpleA.copy(id = id), item)

        timerRepository.delete(id)

        assertNull(timerRepository.item(id))
    }
}
