package xyz.aprildown.timer.presentation.stream

import android.net.Uri
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FlashlightAction
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.domain.usecases.record.AddTimerStamp
import xyz.aprildown.timer.domain.usecases.timer.GetTimer

class MachinePresenterUnitTest {

    private var isForeNotifCreated = false
    private var isForeNotifShowing = false
    private var isForeNotifUpdated = false

    private var foregroundNotifId: Int = -1

    private var createdTimerId: Int = -1
    private var canceledTimerId: Int = -1

    private var prepared = false

    private fun TestScope.getMachine(): MachinePresenter {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val machine = MachinePresenter(
            dispatcher,
            mock(),
            GetTimer(dispatcher, mock()),
            AddTimerStamp(dispatcher, mock(), mock(), mock()),
            mock(),
            mock(),
        )
        machine.takeView(MachineTestView())
        return machine
    }

    @Test
    fun `NotifState, yes1 start, yes1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)

        machine.beginTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes2 end, yes1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(true)

        machine.beginTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes1 end, yes2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(true)

        machine.beginTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, no2 end, yes1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(false)

        machine.beginTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(id, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, yes1 end, no2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(false)

        machine.beginTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifUpdated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes3 start, yes2 end, yes1 end, yes3 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(true)
        val id3 = machine.addThirdTimer(true)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id3, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, no3 start, yes2 end, yes1 end, no3 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(true)
        val id3 = machine.addThirdTimer(false)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, no3 start, yes2 end, no3 end, yes1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(true)
        val id3 = machine.addThirdTimer(false)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, no3 start, yes1 end, no3 end, yes2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(false)
        val id3 = machine.addThirdTimer(false)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no start, no end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)

        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no2 end, no1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(false)

        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no1 end, no2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(false)

        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, yes2 end, no1 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(true)

        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, no1 end, yes2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(true)

        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no3 start, no2 end, no1 end, no3 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(false)
        val id3 = machine.addThirdTimer(false)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, yes3 start, no1 end, no2 end, yes3 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(false)
        val id3 = machine.addThirdTimer(true)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id3, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, yes3 start, no1 end, yes3 end, yes2 end`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(false)
        val id2 = machine.addSecondTimer(true)
        val id3 = machine.addThirdTimer(true)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id3)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `Ultimate complicated tests`() = runTest {
        val machine = getMachine()

        val id = machine.addFirstTimer(true)
        val id2 = machine.addSecondTimer(false)
        val id3 = machine.addThirdTimer(false)
        val id4 = addFourthTimer(machine, true)

        machine.beginTestTimer(id)
        machine.beginTestTimer(id2)
        machine.beginTestTimer(id3)
        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.beginTestTimer(id4)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id4, createdTimerId)
        assertTrue(prepared)

        machine.addFirstTimer(true)
        machine.beginTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        machine.endTestTimer(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id4)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id4, canceledTimerId)
        assertTrue(prepared)

        machine.addSecondTimer(false)
        machine.beginTestTimer(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        machine.endTestTimer(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(machine.isInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        machine.endTestTimer(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    private fun MachinePresenter.addFirstTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleA.copy(more = TimerMoreEntity(showNotif = showNotif))
            .toMachineTimers(this).id
    }

    private fun MachinePresenter.addSecondTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(more = TimerMoreEntity(showNotif = showNotif))
            .toMachineTimers(this).id
    }

    private fun MachinePresenter.addThirdTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(
            id = TestData.fakeTimerId * 2,
            more = TimerMoreEntity(showNotif = showNotif)
        ).toMachineTimers(this).id
    }

    private fun addFourthTimer(machine: MachinePresenter, showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(
            id = TestData.fakeTimerId * 3,
            more = TimerMoreEntity(showNotif = showNotif)
        ).toMachineTimers(machine).id
    }

    private fun TimerEntity.toMachineTimers(machine: MachinePresenter): TimerEntity = apply {
        machine.timers[id] = MachinePresenter.TimerMachinePair(
            timer = this,
            machine = TimerMachine(this, mock())
        )
    }

    /**
     * Copies the method content from [MachinePresenter.begin]
     */
    private fun MachinePresenter.beginTestTimer(id: Int) {
        timerBeginsAction(id)
    }

    /**
     * Copies the method content from [MachinePresenter.end]
     */
    private fun MachinePresenter.endTestTimer(id: Int) {
        val shouldUpdateForeNotif = timerEndsAction(id)

        timers.remove(id)

        if (shouldUpdateForeNotif) {
            updateForeNotifIfPossible()
        }
        stopMachineServiceIfNotRunning()
    }

    private inner class MachineTestView : MachineContract.View {

        override fun prepareForWork() {
            prepared = true
        }

        override fun cleanUpWorkArea() {
            prepared = false
        }

        override fun createForegroundNotif() {
            isForeNotifCreated = true
        }

        override fun updateForegroundNotif(
            totalTimersCount: Int,
            pausedTimersCount: Int,
            theOnlyTimerName: String?
        ) {
            isForeNotifUpdated = true
        }

        override fun cancelForegroundNotif() {
            isForeNotifShowing = false
        }

        override fun createTimerNotification(id: Int, timer: TimerEntity) {
            createdTimerId = id
        }

        override fun cancelTimerNotification(id: Int) {
            canceledTimerId = id
        }

        override fun stopForegroundState() = Unit

        override fun toForeground(id: Int) {
            isForeNotifShowing = id == -1
            foregroundNotifId = id
        }

        override fun playMusic(uri: Uri, loop: Boolean) {
            throw IllegalAccessException("Nope")
        }

        override fun stopMusic() {
            throw IllegalAccessException("Nope")
        }

        override fun startVibrating(pattern: LongArray, repeat: Boolean) {
            throw IllegalAccessException("Nope")
        }

        override fun stopVibrating() {
            throw IllegalAccessException("Nope")
        }

        override fun showScreen(
            timerItem: TimerEntity,
            currentStepName: String,
            fullScreen: Boolean
        ) {
            throw IllegalAccessException("Nope")
        }

        override fun closeScreen() {
            throw IllegalAccessException("Nope")
        }

        override fun beginReading(
            content: CharSequence?,
            contentRes: Int,
            sayMore: Boolean,
            afterDone: (() -> Unit)?
        ) {
            throw IllegalAccessException("Nope")
        }

        override fun formatDuration(duration: Long): CharSequence {
            throw IllegalAccessException("Nope")
        }

        override fun formatTime(time: Long): CharSequence {
            throw IllegalAccessException("Nope")
        }

        override fun stopReading() {
            throw IllegalAccessException("Nope")
        }

        override fun enableTone(tone: Int, count: Int, respectOtherSound: Boolean) {
            throw IllegalAccessException("Nope")
        }

        override fun playTone() {
            throw IllegalAccessException("Nope")
        }

        override fun disableTone() {
            throw IllegalAccessException("Nope")
        }

        override fun showBehaviourNotification(
            timer: TimerEntity,
            index: TimerIndex,
            duration: Int
        ) {
            throw IllegalAccessException("Nope")
        }

        override fun toggleFlashlight(action: FlashlightAction?, duration: Long) {
            throw IllegalAccessException("Nope")
        }

        override fun dismissBehaviourNotification() {
            throw IllegalAccessException("Nope")
        }

        override fun finish() = Unit

        override fun begin(timerId: Int) = Unit

        override fun started(timerId: Int, index: TimerIndex) {
            throw IllegalAccessException("Nope")
        }

        override fun paused(timerId: Int) {
            throw IllegalAccessException("Nope")
        }

        override fun updated(timerId: Int, time: Long) {
            throw IllegalAccessException("Nope")
        }

        override fun finished(timerId: Int) {
            throw IllegalAccessException("Nope")
        }

        override fun end(timerId: Int, forced: Boolean) = Unit
    }
}
