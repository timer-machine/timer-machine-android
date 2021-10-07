package xyz.aprildown.timer.presentation.stream

import android.content.Context
import android.net.Uri
import xyz.aprildown.timer.domain.entities.FlashlightAction
import xyz.aprildown.timer.domain.entities.TimerEntity

/**
 * (x, y) => (horizontal, vertical) index
 * |no show|          0        |          1           |          2        |        more
 * |0      |NoNotif            |SingleTimer           |ForeNotif          |ForeNotif|
 * |1      |ForeNotif          |ForeNotif             |ForeNotif          |ForeNotif|
 * |2      |ForeNotif          |ForeNotif             |ForeNotif          |ForeNotif|
 * |more   |ForeNotif          |ForeNotif             |ForeNotif          |ForeNotif|
 *
 *         show    noShow
 * begin: right     down
 * end:    left      up
 */
sealed class NotifState

internal object NoNotif : NotifState()
internal object SingleTimer : NotifState()
internal object ForeNotif : NotifState()

/**
 * Has to be here since this contract use [StreamState] which is Android specific.
 */
interface MachineContract {
    /**
     * Handles [Context] related actions
     * Updates Notifications
     */
    interface View : TimerMachineListener {
        fun prepareForWork()
        fun cleanUpWorkArea()

        fun createForegroundNotif()
        fun updateForegroundNotif(
            totalTimersCount: Int,
            pausedTimersCount: Int,
            theOnlyTimerName: String? = null
        )

        fun cancelForegroundNotif()

        fun createTimerNotification(id: Int, timer: TimerEntity)
        fun cancelTimerNotification(id: Int)

        fun stopForegroundState()

        fun toForeground(id: Int = -1)

        fun playMusic(uri: Uri, loop: Boolean)
        fun stopMusic()

        fun startVibrating(pattern: LongArray, repeat: Boolean)
        fun stopVibrating()

        fun showScreen(timerItem: TimerEntity, currentStepName: String, fullScreen: Boolean)
        fun closeScreen()

        // Halt is handled in the presenter

        fun beginReading(
            content: CharSequence? = null,
            contentRes: Int = 0,
            sayMore: Boolean = false,
            afterDone: (() -> Unit)? = null
        )

        fun formatDuration(duration: Long): CharSequence
        fun formatTime(time: Long): CharSequence

        fun stopReading()

        fun enableTone(tone: Int, count: Int, respectOtherSound: Boolean)
        fun playTone()
        fun disableTone()

        fun showBehaviourNotification(timer: TimerEntity, index: TimerIndex, duration: Int)
        fun dismissBehaviourNotification()

        fun toggleFlashlight(action: FlashlightAction?, duration: Long = 0L)

        fun finish()
    }

    class CurrentTimerInfo(
        val timerEntity: TimerEntity,
        val state: StreamState,
        val index: TimerIndex,
        val time: Long
    )

    interface Presenter {
        var view: View?
        var isInTheForeground: Boolean
        var currentNotifState: NotifState

        fun takeView(view: View)
        fun dropView()

        fun addListener(timerId: Int, listener: TimerMachineListener)
        fun removeListener(timerId: Int, listener: TimerMachineListener)

        fun addAllListener(listener: TimerMachineListener)
        fun removeAllListener(listener: TimerMachineListener)

        /**
         * @return null if this timer isn't running.
         */
        fun getTimerStateInfo(id: Int): CurrentTimerInfo?

        fun startTimer(timerId: Int, index: TimerIndex? = null)
        fun pauseTimer(timerId: Int)
        fun moveTimer(timerId: Int, index: TimerIndex)
        fun decreTimer(timerId: Int)
        fun increTimer(timerId: Int)
        fun resetTimer(timerId: Int)
        fun adjustAmount(timerId: Int, amount: Long, goBackOnNotifier: Boolean)

        fun startAll()
        fun pauseAll(): List<Int>
        fun stopAll()

        fun scheduleStart(timerId: Int)
        fun scheduleEnd(timerId: Int)
    }

    interface PresenterProvider {
        fun getPresenter(): Presenter
    }
}
