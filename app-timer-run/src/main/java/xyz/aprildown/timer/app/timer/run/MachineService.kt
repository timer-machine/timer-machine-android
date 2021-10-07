package xyz.aprildown.timer.app.timer.run

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.TtsSpan
import android.util.SparseArray
import androidx.core.app.NotificationCompat
import androidx.core.os.postDelayed
import androidx.core.text.buildSpannedString
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import xyz.aprildown.timer.app.base.data.PreferenceData.shouldNotifierPlusGoBack
import xyz.aprildown.timer.app.base.data.PreferenceData.shouldPausePhoneCall
import xyz.aprildown.timer.app.base.data.PreferenceData.shouldResumePhoneCall
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioFocusType
import xyz.aprildown.timer.app.base.data.PreferenceData.storedAudioTypeValue
import xyz.aprildown.timer.app.base.media.Beeper
import xyz.aprildown.timer.app.base.media.RingtonePreviewKlaxon
import xyz.aprildown.timer.app.base.media.Torch
import xyz.aprildown.timer.app.base.media.TtsSpeaker
import xyz.aprildown.timer.app.base.media.VibrateHelper
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.utils.ScreenWakeLock
import xyz.aprildown.timer.app.base.utils.produceHms
import xyz.aprildown.timer.app.timer.run.receiver.SchedulerReceiver
import xyz.aprildown.timer.app.timer.run.screen.ScreenActivity
import xyz.aprildown.timer.domain.entities.FlashlightAction
import xyz.aprildown.timer.domain.entities.SchedulerEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.utils.AppTracker
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getTimerIndex
import xyz.aprildown.timer.presentation.stream.putTimerIndex
import xyz.aprildown.tools.helper.HandlerHelper
import xyz.aprildown.tools.helper.getNumberFormattedQuantityString
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * DO NOT CALL STOP_SERVICE ON YOUR OWN.
 * Reset every timer and this service will be stopped automatically.
 */
@AndroidEntryPoint
class MachineService : Service(),
    MachineContract.View,
    PhoneCallReceiver.ServiceActionCallback {

    private var binder: IBinder? = null

    class MachineBinder(
        private val presenter: MachineContract.Presenter
    ) : Binder(), MachineContract.PresenterProvider {
        override fun getPresenter(): MachineContract.Presenter = presenter
    }

    @Inject
    lateinit var presenter: MachineContract.Presenter

    @Inject
    lateinit var appNavigator: AppNavigator

    @Inject
    lateinit var appTracker: AppTracker

    private lateinit var notificationManager: NotificationManager

    private var foregroundNotifId: Int = 0
    private var foregroundNotifBuilder: NotificationCompat.Builder? = null
    private val updaterMap = SparseArray<MachineNotif>()

    private var phoneCallReceiver: PhoneCallReceiver? = null
    private var phoneCallPausedTimerIds: List<Int>? = null

    private var toForegroundHandler: Handler = Handler(Looper.getMainLooper())
    private var foregroundNotifHandler: Handler? = null

    override fun onCreate() {
        super.onCreate()
        binder = MachineBinder(presenter)
        presenter.takeView(this@MachineService)
        presenter.addAllListener(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.buildChannelIfNecessary(this)
    }

    override fun onBind(intent: Intent): IBinder? = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timerId = intent?.getIntExtra(EXTRA_TIMER_ID, TimerEntity.NULL_ID)
            ?: TimerEntity.NULL_ID
        when (intent?.action) {
            ACTION_START -> presenter.startTimer(
                timerId,
                intent.getTimerIndex()
            )
            ACTION_PAUSE -> presenter.pauseTimer(timerId)
            ACTION_MOVE -> intent.getTimerIndex()?.let { index ->
                presenter.moveTimer(timerId, index)
            }
            ACTION_DECRE -> presenter.decreTimer(timerId)
            ACTION_INCRE -> presenter.increTimer(timerId)
            ACTION_RESET -> presenter.resetTimer(timerId)
            ACTION_ADJUST_AMOUNT -> presenter.adjustAmount(
                timerId,
                intent.getLongExtra(EXTRA_AMOUNT, 0L),
                shouldNotifierPlusGoBack
            )

            ACTION_START_ALL -> presenter.startAll()
            ACTION_PAUSE_ALL -> presenter.pauseAll()
            ACTION_STOP_ALL -> presenter.stopAll()

            ACTION_TIMER_SCHEDULE_START -> presenter.scheduleStart(timerId)
            ACTION_TIMER_SCHEDULE_END -> presenter.scheduleEnd(timerId)

            else -> {
                // Handle legacy requests
                val schedulerId = intent?.getIntExtra(EXTRA_SCHEDULER_ID, SchedulerEntity.NULL_ID)
                if (schedulerId != null && schedulerId != SchedulerEntity.NULL_ID) {
                    when (intent.action) {
                        ACTION_SCHEDULER_START -> SchedulerEntity.ACTION_START
                        ACTION_SCHEDULER_END -> SchedulerEntity.ACTION_END
                        else -> null
                    }?.let {
                        sendBroadcast(SchedulerReceiver.intent(this, schedulerId, it))
                    }
                }
            }
        }
        return START_STICKY
    }

    /**
     * Free all resources and service is dead
     */
    override fun finish() {
        toForegroundHandler.removeCallbacksAndMessages(null)
        stopForeground(true)
        notificationManager.cancelAll()
        updaterMap.clear()
        stopSelf()
    }

    /**
     * Just release all resources
     */
    override fun onDestroy() {
        super.onDestroy()
        presenter.dropView()
    }

    //
    // TimerCallback
    //

    override fun prepareForWork() {
        ServiceWakeLock.acquireCpuWakeLock(this)
        ScreenWakeLock.acquireScreenWakeLock(
            context = this,
            screenTiming = getString(R.string.pref_screen_timing_value_service)
        )
        if (shouldPausePhoneCall) {
            phoneCallReceiver?.unListen()
            phoneCallReceiver = PhoneCallReceiver(this).apply {
                listen(this@MachineService)
            }
            phoneCallPausedTimerIds = null
        }
    }

    override fun pauseActionsForCalls() {
        if (shouldPausePhoneCall) {
            HandlerHelper.runOnUiThread {
                phoneCallPausedTimerIds = presenter.pauseAll()
            }
        }
    }

    override fun resumeActionsAfterCalls() {
        if (shouldResumePhoneCall) {
            HandlerHelper.runOnUiThread {
                phoneCallPausedTimerIds?.forEach { timerId ->
                    presenter.startTimer(timerId)
                }
                phoneCallPausedTimerIds = null
            }
        }
    }

    override fun cleanUpWorkArea() {
        ServiceWakeLock.releaseCpuLock()
        ScreenWakeLock.releaseScreenLock(
            context = this,
            screenTiming = getString(R.string.pref_screen_timing_value_service)
        )
        phoneCallReceiver?.unListen()
        phoneCallReceiver = null
        phoneCallPausedTimerIds = null
    }

    override fun createForegroundNotif() {
        foregroundNotifHandler = Handler(Looper.getMainLooper())
        foregroundNotifBuilder = serviceBuilder(
            appNavigator = appNavigator,
            totalRunningTimerCount = 0,
            pausedTimerCount = 0
        )
    }

    override fun cancelForegroundNotif() {
        foregroundNotifHandler?.removeCallbacksAndMessages(null)
        foregroundNotifHandler = null
        foregroundNotifBuilder = null
    }

    override fun updateForegroundNotif(
        totalTimersCount: Int,
        pausedTimersCount: Int,
        theOnlyTimerName: String?
    ) {
        foregroundNotifHandler?.removeCallbacksAndMessages(null)
        // You'll get 3 frames.
        foregroundNotifHandler?.postDelayed(48) {
            val builder = serviceBuilder(
                appNavigator = appNavigator,
                totalRunningTimerCount = totalTimersCount,
                pausedTimerCount = pausedTimersCount,
                theOnlyTimerName = theOnlyTimerName
            )
            foregroundNotifBuilder = builder
            notificationManager.notify(foregroundNotifId, builder.build())
        }
    }

    override fun createTimerNotification(id: Int, timer: TimerEntity) {
        val notif = TimerNotif(
            context = this,
            appNavigator = appNavigator,
            timer = timer
        )
        notif.createStub()
        updaterMap.put(id, notif)
    }

    @Synchronized
    override fun toForeground(id: Int) {
        toForegroundHandler.removeCallbacksAndMessages(null)
        toForegroundHandler.postDelayed(16) {
            if (id != -1) {
                foregroundNotifId = id
                updaterMap[id]?.builder?.build()?.let {
                    startForeground(foregroundNotifId, it)
                }
            } else {
                foregroundNotifId = Constants.NOTIF_ID_SERVICE
                foregroundNotifBuilder?.build()?.let {
                    startForeground(foregroundNotifId, it)
                }
            }
        }
    }

    override fun cancelTimerNotification(id: Int) {
        notificationManager.cancel(id)
        updaterMap.remove(id)
    }

    override fun stopForegroundState() {
        stopForeground(true)
    }

    // region Timer Notification Updates

    override fun begin(timerId: Int) = Unit

    override fun started(timerId: Int, index: TimerIndex) {
        updaterMap[timerId]?.start(index)?.build()?.let {
            notificationManager.notify(timerId, it)
        }
    }

    override fun paused(timerId: Int) {
        updaterMap[timerId]?.pause()?.build()?.let {
            notificationManager.notify(timerId, it)
        }
    }

    override fun updated(timerId: Int, time: Long) {
        // https://stackoverflow.com/a/43385751/5507158
        try {
            updaterMap[timerId]?.update(time)?.build()?.let {
                notificationManager.notify(timerId, it)
            }
        } catch (e: Exception) {
            appTracker.trackError(e)
        }
    }

    override fun finished(timerId: Int) = Unit

    override fun end(timerId: Int, forced: Boolean) = Unit

    // endregion Timer Notification Updates

    //
    // Timer Behaviour
    //

    override fun playMusic(uri: Uri, loop: Boolean) {
        RingtonePreviewKlaxon.start(
            context = this,
            uri = uri,
            crescendoDuration = 250,
            loop = loop,
            audioFocusType = storedAudioFocusType,
            streamType = storedAudioTypeValue
        )
    }

    override fun stopMusic() {
        RingtonePreviewKlaxon.stop(this)
    }

    override fun startVibrating(pattern: LongArray, repeat: Boolean) {
        VibrateHelper.start(this, pattern, repeat)
    }

    override fun stopVibrating() {
        VibrateHelper.stop(this)
    }

    override fun showScreen(timerItem: TimerEntity, currentStepName: String, fullScreen: Boolean) {
        if (fullScreen) {
            startActivity(
                ScreenActivity.intent(
                    this,
                    timerItem.id,
                    timerItem.name,
                    currentStepName
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            notificationManager.notify(
                Constants.NOTIF_ID_SCREEN,
                buildScreenNotificationBuilder(timerItem, currentStepName).build()
            )
        }
    }

    override fun closeScreen() {
        notificationManager.cancel(Constants.NOTIF_ID_SCREEN)
        ScreenActivity.screen?.finish()
    }

    override fun beginReading(
        content: CharSequence?,
        contentRes: Int,
        sayMore: Boolean,
        afterDone: (() -> Unit)?
    ) {
        TtsSpeaker.speak(
            this,
            content ?: getString(contentRes),
            sayMore = sayMore,
            callback = object : TtsSpeaker.Callback() {
                override fun onDone() {
                    afterDone?.invoke()
                }

                override fun onError() {
                    afterDone?.invoke()
                }
            }
        )
    }

    override fun formatDuration(duration: Long): CharSequence {
        val (hours, minutes, seconds) = duration.produceHms()
        return buildString {
            if (hours > 0) {
                append(getNumberFormattedQuantityString(R.plurals.hours, hours))
            }
            if (minutes > 0) {
                if (isNotEmpty()) {
                    append(", ")
                }
                append(getNumberFormattedQuantityString(R.plurals.minutes, minutes))
            }
            if (seconds > 0) {
                if (isNotEmpty()) {
                    append(", ")
                }
                append(getNumberFormattedQuantityString(R.plurals.seconds, seconds))
            }
            if (isEmpty()) {
                append(getString(R.string.seconds_0))
            }
        }
    }

    override fun formatTime(time: Long): CharSequence {
        val localTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
        return buildSpannedString {
            append(
                DateUtils.formatDateTime(this@MachineService, time, DateUtils.FORMAT_SHOW_TIME),
                TtsSpan.TimeBuilder(localTime.hour, localTime.minute).build(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun stopReading() {
        TtsSpeaker.tearDown()
    }

    override fun enableTone(tone: Int, count: Int, respectOtherSound: Boolean) {
        fun load() {
            Beeper.load(
                Beeper.Settings(
                    audioFocusType = storedAudioFocusType,
                    streamType = storedAudioTypeValue,
                    sound = tone,
                    count = count,
                    respectOther = respectOtherSound
                )
            )
        }

        try {
            load()
        } catch (e: Exception) {
            Timber.e(e)
            try {
                load()
            } catch (e: Exception) {
                appTracker.trackError(e)
            }
        }
    }

    override fun playTone() {
        if (Beeper.isLoaded) {
            Beeper.play(this)
        }
    }

    override fun disableTone() {
        Beeper.tearDown()
    }

    override fun showBehaviourNotification(timer: TimerEntity, index: TimerIndex, duration: Int) {
        dismissBehaviourNotification()
        notificationManager.notify(
            Constants.NOTIF_ID_NOTIFICATION,
            buildBehaviourNotification(
                appNavigator = appNavigator,
                context = this,
                timer = timer,
                index = index,
                duration = duration
            ).build()
        )
    }

    override fun toggleFlashlight(action: FlashlightAction?, duration: Long) {
        if (action != null) {
            Torch.start(this, duration, action.step)
        } else {
            Torch.stop()
        }
    }

    override fun dismissBehaviourNotification() {
        notificationManager.cancel(Constants.NOTIF_ID_NOTIFICATION)
    }

    companion object {
        private const val ACTION_PREFIX = "COMMAND"
        private const val ACTION_START = "${ACTION_PREFIX}_START"
        private const val ACTION_PAUSE = "${ACTION_PREFIX}_PAUSE"
        private const val ACTION_MOVE = "${ACTION_PREFIX}_MOVE"
        private const val ACTION_DECRE = "${ACTION_PREFIX}_DECRE"
        private const val ACTION_INCRE = "${ACTION_PREFIX}_INCRE"
        private const val ACTION_RESET = "${ACTION_PREFIX}_RESET"
        private const val ACTION_ADJUST_AMOUNT = "${ACTION_PREFIX}_ADJUST_AMOUNT"

        private const val ACTION_START_ALL = "${ACTION_PREFIX}_START_ALL"
        private const val ACTION_PAUSE_ALL = "${ACTION_PREFIX}_PAUSE_ALL"
        private const val ACTION_STOP_ALL = "${ACTION_PREFIX}_STOP_ALL"

        private const val ACTION_SCHEDULER_START = "${ACTION_PREFIX}_SCHEDULER_START"
        private const val ACTION_SCHEDULER_END = "${ACTION_PREFIX}_SCHEDULER_END"
        private const val ACTION_TIMER_SCHEDULE_START = "${ACTION_PREFIX}_TIMER_SCHEDULE_START"
        private const val ACTION_TIMER_SCHEDULE_END = "${ACTION_PREFIX}_TIMER_SCHEDULE_END"

        private const val EXTRA_PREFIX = "EXTRA_"
        private const val EXTRA_TIMER_ID = "${EXTRA_PREFIX}ID"
        private const val EXTRA_AMOUNT = "${EXTRA_PREFIX}AMOUNT"

        private const val EXTRA_SCHEDULER_ID = "${EXTRA_PREFIX}SCHEDULER_ID"

        private fun pureIntent(context: Context): Intent =
            Intent(context, MachineService::class.java)

        fun startTimingIntent(
            context: Context,
            itemId: Int,
            index: TimerIndex? = null
        ): Intent =
            Intent(context, MachineService::class.java).setAction(ACTION_START)
                .putExtra(EXTRA_TIMER_ID, itemId)
                .putTimerIndex(index)

        fun pauseTimingIntent(context: Context, itemId: Int): Intent =
            pureIntent(context).setAction(ACTION_PAUSE)
                .putExtra(EXTRA_TIMER_ID, itemId)

        fun decreTimingIntent(context: Context, itemId: Int): Intent =
            pureIntent(context).setAction(ACTION_DECRE)
                .putExtra(EXTRA_TIMER_ID, itemId)

        fun increTimingIntent(context: Context, itemId: Int): Intent =
            pureIntent(context).setAction(ACTION_INCRE)
                .putExtra(EXTRA_TIMER_ID, itemId)

        fun moveTimingIntent(context: Context, itemId: Int, index: TimerIndex): Intent =
            pureIntent(context).setAction(ACTION_MOVE)
                .putExtra(EXTRA_TIMER_ID, itemId)
                .putTimerIndex(index)

        fun resetTimingIntent(context: Context, itemId: Int): Intent =
            pureIntent(context).setAction(ACTION_RESET)
                .putExtra(EXTRA_TIMER_ID, itemId)

        fun adjustAmountIntent(context: Context, timerId: Int, amount: Long): Intent =
            pureIntent(context).setAction(ACTION_ADJUST_AMOUNT)
                .putExtra(EXTRA_TIMER_ID, timerId)
                .putExtra(EXTRA_AMOUNT, amount)

        fun startAllIntent(context: Context): Intent =
            pureIntent(context).setAction(ACTION_START_ALL)

        fun pauseAllIntent(context: Context): Intent =
            pureIntent(context).setAction(ACTION_PAUSE_ALL)

        fun stopAllIntent(context: Context): Intent =
            pureIntent(context).setAction(ACTION_STOP_ALL)

        fun scheduleTimerStartIntent(context: Context, timerId: Int): Intent =
            pureIntent(context).setAction(ACTION_TIMER_SCHEDULE_START)
                .putExtra(EXTRA_TIMER_ID, timerId)

        fun scheduleTimerEndIntent(context: Context, timerId: Int): Intent =
            pureIntent(context).setAction(ACTION_TIMER_SCHEDULE_END)
                .putExtra(EXTRA_TIMER_ID, timerId)

        fun bindIntent(context: Context) = pureIntent(context)
    }
}
