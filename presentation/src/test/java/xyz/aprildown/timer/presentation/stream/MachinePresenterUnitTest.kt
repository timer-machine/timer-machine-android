package xyz.aprildown.timer.presentation.stream

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FlashlightAction
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerMoreEntity
import xyz.aprildown.timer.domain.usecases.record.AddTimerStamp
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import xyz.aprildown.timer.presentation.testCoroutineDispatcher

class MachinePresenterUnitTest {

    private lateinit var machine: MachinePresenter
    private lateinit var view: MachineTestView

    private val isServiceInTheForeground get() = machine.isInTheForeground

    private var isForeNotifCreated = false
    private var isForeNotifShowing = false
    private var isForeNotifUpdated = false

    private var foregroundNotifId: Int = -1

    private var createdTimerId: Int = -1
    private var canceledTimerId: Int = -1

    private var prepared = false

    @Before
    fun setUp() {
        machine = MachinePresenter(
            testCoroutineDispatcher,
            mock(),
            GetTimer(testCoroutineDispatcher, mock()),
            AddTimerStamp(testCoroutineDispatcher, mock(), mock(), mock()),
            mock(),
        )
        view = MachineTestView()
        machine.takeView(view)
    }

    @Test
    fun `NotifState, yes1 start, yes1 end`() {
        val id = addFirstTimer(true)

        begin(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes2 end, yes1 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(true)

        begin(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes1 end, yes2 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(true)

        begin(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, no2 end, yes1 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(false)

        begin(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(id, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, yes1 end, no2 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(false)

        begin(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifCreated)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertFalse(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifUpdated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, yes3 start, yes2 end, yes1 end, yes3 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(true)
        val id3 = addThirdTimer(true)

        begin(id)
        begin(id2)
        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id3, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        end(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, no3 start, yes2 end, yes1 end, no3 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(true)
        val id3 = addThirdTimer(false)

        begin(id)
        begin(id2)
        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        end(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, yes2 start, no3 start, yes2 end, no3 end, yes1 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(true)
        val id3 = addThirdTimer(false)

        begin(id)
        begin(id2)
        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        end(id3)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, yes1 start, no2 start, no3 start, yes1 end, no3 end, yes2 end`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(false)
        val id3 = addThirdTimer(false)

        begin(id)
        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        end(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no start, no end`() {
        val id = addFirstTimer(false)

        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no2 end, no1 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(false)

        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no1 end, no2 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(false)

        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, yes2 end, no1 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(true)

        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, canceledTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, no1 end, yes2 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(true)

        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(machine.isInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, no3 start, no2 end, no1 end, no3 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(false)
        val id3 = addThirdTimer(false)

        begin(id)
        begin(id2)
        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, no2 start, yes3 start, no1 end, no2 end, yes3 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(false)
        val id3 = addThirdTimer(true)

        begin(id)
        begin(id2)
        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id3, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        end(id3)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `NotifState, no1 start, yes2 start, yes3 start, no1 end, yes3 end, yes2 end`() {
        val id = addFirstTimer(false)
        val id2 = addSecondTimer(true)
        val id3 = addThirdTimer(true)

        begin(id)
        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id2, createdTimerId)
        assertTrue(prepared)

        begin(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id3, createdTimerId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id3)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id2, foregroundNotifId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    @Test
    fun `Ultimate complicated tests`() {
        val id = addFirstTimer(true)
        val id2 = addSecondTimer(false)
        val id3 = addThirdTimer(false)
        val id4 = addFourthTimer(true)

        begin(id)
        begin(id2)
        begin(id3)
        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        begin(id4)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id4, createdTimerId)
        assertTrue(prepared)

        addFirstTimer(true)
        begin(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifCreated)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, createdTimerId)
        assertTrue(prepared)

        end(id2)
        end(id3)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id4)
        assertEquals(SingleTimer, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertEquals(id, foregroundNotifId)
        assertEquals(id4, canceledTimerId)
        assertTrue(prepared)

        addSecondTimer(false)
        begin(id2)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertEquals(-1, foregroundNotifId)
        assertTrue(prepared)

        end(id)
        assertEquals(ForeNotif, machine.currentNotifState)
        assertTrue(isServiceInTheForeground)
        assertTrue(isForeNotifShowing)
        assertTrue(isForeNotifUpdated)
        assertEquals(-1, foregroundNotifId)
        assertEquals(id, canceledTimerId)
        assertTrue(prepared)

        end(id2)
        assertEquals(NoNotif, machine.currentNotifState)
        assertFalse(isServiceInTheForeground)
        assertFalse(isForeNotifShowing)
        assertFalse(prepared)
    }

    private fun addFirstTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleA.copy(more = TimerMoreEntity(showNotif = showNotif))
            .toMachineTimers().id
    }

    private fun addSecondTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(more = TimerMoreEntity(showNotif = showNotif))
            .toMachineTimers().id
    }

    private fun addThirdTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(
            id = TestData.fakeTimerId * 2,
            more = TimerMoreEntity(showNotif = showNotif)
        ).toMachineTimers().id
    }

    private fun addFourthTimer(showNotif: Boolean): Int {
        return TestData.fakeTimerSimpleB.copy(
            id = TestData.fakeTimerId * 3,
            more = TimerMoreEntity(showNotif = showNotif)
        ).toMachineTimers().id
    }

    private fun TimerEntity.toMachineTimers(): TimerEntity = apply {
        machine.timers[id] = MachinePresenter.TimerMachinePair(
            this, TimerMachine(this, mock())
        )
    }

    /**
     * Copies the method content from [MachinePresenter.begin]
     */
    private fun begin(id: Int) {
        machine.timerBeginsAction(id)
    }

    /**
     * Copies the method content from [MachinePresenter.end]
     */
    private fun end(id: Int) {
        val shouldUpdateForeNotif = machine.timerEndsAction(id)

        machine.timers.remove(id)

        if (shouldUpdateForeNotif) {
            machine.updateForeNotifIfPossible()
        }
        machine.stopMachineServiceIfNotRunning()
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
