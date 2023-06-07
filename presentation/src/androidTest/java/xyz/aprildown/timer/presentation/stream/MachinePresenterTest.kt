package xyz.aprildown.timer.presentation.stream

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import xyz.aprildown.timer.domain.TestData
import xyz.aprildown.timer.domain.entities.FlashlightAction
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.repositories.TimerRepository
import xyz.aprildown.timer.domain.usecases.record.AddTimerStamp
import xyz.aprildown.timer.domain.usecases.timer.GetTimer
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
@LargeTest
class MachinePresenterTest {

    private val timerRepository: TimerRepository = mock()
    private val appDataRepository: AppDataRepository = mock()

    private fun getPresenterView(): Pair<MachinePresenter, TestView> {
        val dispatcher = Dispatchers.Main.immediate
        val presenter = MachinePresenter(
            dispatcher,
            mock(),
            GetTimer(dispatcher, timerRepository),
            AddTimerStamp(dispatcher, mock(), mock(), appDataRepository),
            mock()
        )
        val view = TestView()
        presenter.takeView(view)
        presenter.addAllListener(view)
        return presenter to view
    }

    @Test
    fun action1() = runTest(timeout = 1.minutes) {
        val (presenter, view) = getPresenterView()

        val t = TestData.fakeTimerSimpleB
        val id = t.id
        whenever(timerRepository.item(id)).thenReturn(t)
        withContext(Dispatchers.Main) {
            assertFalse(view.running)

            presenter.startTimer(id)
            verify(timerRepository).item(id)
            // Start step
            assertTrue(view.running)
            assertTrue(view.timerIdAndState.containsKey(id))
            assertTrue(view.timerIdAndState[id]?.isRunning ?: false)
            assertFalse(view.playingMusic)
            assertFalse(view.vibrating)
            assertFalse(view.showingScreen)
            assertFalse(view.reading)
            delay(100)
            assertTrue(view.remaining in 58_000..60_000)
            delay(5_000)
            assertTrue(view.remaining in 54_000..56_000)

            presenter.pauseTimer(id)
            assertTrue(view.running)
            assertTrue(view.timerIdAndState[id]?.isPaused ?: false)
            assertTrue(view.remaining in 54_000..56_000)

            presenter.startTimer(id)
            assertTrue(view.running)
            assertTrue(view.timerIdAndState[id]?.isRunning ?: false)
            delay(5_000)
            assertTrue(view.remaining in 49_000..54_000)

            // To second step
            presenter.increTimer(id)
            presenter.increTimer(id)
            assertTrue(view.running)
            assertTrue(view.timerIdAndState[id]?.isRunning ?: false)
            assertTrue(view.playingMusic)
            assertTrue(view.vibrating)
            assertFalse(view.showingScreen)
            assertFalse(view.reading)
            delay(100)
            assertTrue(view.remaining in 4_500..5_100)

            // plus one causing go back to the last step
            presenter.adjustAmount(id, 60_000L, true)
            assertTrue(view.running)
            assertTrue(view.timerIdAndState[id]?.isRunning ?: false)
            assertFalse(view.playingMusic)
            assertFalse(view.vibrating)
            assertFalse(view.showingScreen)
            assertFalse(view.reading)
            assertTrue(view.remaining in 59_800..60_100)

            presenter.resetTimer(id)
            assertFalse(view.running)
            assertFalse(view.timerIdAndState[id]?.isRunning ?: false)
            assertFalse(view.playingMusic)
            assertFalse(view.vibrating)
            assertFalse(view.showingScreen)
            assertFalse(view.reading)
        }
    }

    private class TestView : MachineContract.View {
        var running = false
        var timerIdAndState = mutableMapOf<Int, StreamState>()
        var remaining = -1L

        var playingMusic = false
        var vibrating = false
        var showingScreen = false
        var reading = false
        var beeping = false

        override fun prepareForWork() = Unit

        override fun cleanUpWorkArea() = Unit

        override fun createTimerNotification(id: Int, timer: TimerEntity) {
            timerIdAndState[id] = StreamState.RESET
        }

        override fun cancelTimerNotification(id: Int) {
            timerIdAndState.remove(id)
        }

        override fun stopForegroundState() {
        }

        override fun createForegroundNotif() {
            running = true
        }

        override fun updateForegroundNotif(
            totalTimersCount: Int,
            pausedTimersCount: Int,
            theOnlyTimerName: String?
        ) = Unit

        override fun cancelForegroundNotif() {
            running = false
        }

        override fun toForeground(id: Int) = Unit

        override fun playMusic(uri: Uri, loop: Boolean) {
            playingMusic = true
        }

        override fun stopMusic() {
            playingMusic = false
        }

        override fun startVibrating(pattern: LongArray, repeat: Boolean) {
            vibrating = true
        }

        override fun stopVibrating() {
            vibrating = false
        }

        override fun showScreen(
            timerItem: TimerEntity,
            currentStepName: String,
            fullScreen: Boolean
        ) {
            showingScreen = true
        }

        override fun closeScreen() {
            showingScreen = false
        }

        override fun beginReading(
            content: CharSequence?,
            contentRes: Int,
            sayMore: Boolean,
            afterDone: (() -> Unit)?
        ) {
            reading = true
        }

        override fun formatDuration(duration: Long): CharSequence = ""
        override fun formatTime(time: Long): CharSequence = ""

        override fun stopReading() {
            reading = false
        }

        override fun enableTone(tone: Int, count: Int, respectOtherSound: Boolean) {
            beeping = true
        }

        override fun playTone() = Unit

        override fun disableTone() {
            beeping = false
        }

        override fun showBehaviourNotification(
            timer: TimerEntity,
            index: TimerIndex,
            duration: Int
        ) = Unit

        override fun toggleFlashlight(action: FlashlightAction?, duration: Long) = Unit

        override fun dismissBehaviourNotification() = Unit

        override fun finish() = Unit

        override fun begin(timerId: Int) {
            timerIdAndState[timerId] = StreamState.RUNNING
        }

        override fun started(timerId: Int, index: TimerIndex) {
            timerIdAndState[timerId] = StreamState.RUNNING
        }

        override fun paused(timerId: Int) {
            timerIdAndState[timerId] = StreamState.PAUSED
        }

        override fun updated(timerId: Int, time: Long) {
            this.remaining = time
        }

        override fun finished(timerId: Int) {
            timerIdAndState[timerId] = StreamState.RESET
        }

        override fun end(timerId: Int, forced: Boolean) {
            timerIdAndState[timerId] = StreamState.RESET
        }
    }
}
