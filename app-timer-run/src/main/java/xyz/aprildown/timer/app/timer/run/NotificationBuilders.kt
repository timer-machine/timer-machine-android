package xyz.aprildown.timer.app.timer.run

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import androidx.core.app.NotificationCompat.Builder
import androidx.core.content.edit
import com.github.deweyreed.tools.helper.pendingActivityIntent
import com.github.deweyreed.tools.helper.pendingServiceIntent
import xyz.aprildown.timer.app.base.data.PreferenceData.getTypeColor
import xyz.aprildown.timer.app.base.data.PreferenceData.useMediaStyleNotification
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.run.screen.ScreenActivity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.domain.utils.Constants.CHANNEL_B_NOTIF
import xyz.aprildown.timer.domain.utils.Constants.CHANNEL_SCREEN
import xyz.aprildown.timer.domain.utils.Constants.CHANNEL_SERVICE
import xyz.aprildown.timer.domain.utils.Constants.CHANNEL_TIMING
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.tools.helper.safeSharedPreference
import xyz.aprildown.timer.app.base.R as RBase

internal fun Context.serviceBuilder(
    appNavigator: AppNavigator,
    totalRunningTimerCount: Int,
    pausedTimerCount: Int,
    theOnlyTimerName: String? = null
): Builder {
    val isAllRunning = pausedTimerCount == 0
    val isAllPaused = totalRunningTimerCount == pausedTimerCount

    val content = when {
        isAllRunning -> {
            if (totalRunningTimerCount > 1) {
                getString(RBase.string.notif_timers_running, totalRunningTimerCount.toString())
            } else {
                getString(RBase.string.notif_timer_running, theOnlyTimerName.toString())
            }
        }
        isAllPaused -> {
            if (totalRunningTimerCount > 1) {
                getString(RBase.string.notif_timers_paused, totalRunningTimerCount.toString())
            } else {
                getString(RBase.string.notif_timer_paused, theOnlyTimerName.toString())
            }
        }
        else -> buildString {
            append(
                getString(
                    RBase.string.notif_timers_running,
                    (totalRunningTimerCount - pausedTimerCount).toString()
                )
            )
            append(" ")
            append(getString(RBase.string.notif_timers_paused, pausedTimerCount.toString()))
        }
    }

    val builder = Builder(this, CHANNEL_SERVICE)
        .setShowWhen(false)
        .setSmallIcon(RBase.drawable.ic_watch)
        .setContentIntent(pendingActivityIntent(appNavigator.getMainIntent()))
        .setOngoing(true)
        .setAutoCancel(false)
        .setLocalOnly(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup("service")
        .setSortKey("a_service")
        .setColor(newDynamicTheme.colorPrimary)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setContentTitle(content)
        builder.addAction(
            RBase.drawable.ic_stop,
            getString(RBase.string.notif_a11y_stop_ally),
            pendingServiceIntent(MachineService.stopAllIntent(this))
        )
        if (isAllPaused) {
            builder.addAction(
                RBase.drawable.ic_start,
                getString(RBase.string.notif_a11y_start_ally),
                pendingServiceIntent(MachineService.startAllIntent(this))
            )
        } else {
            builder.addAction(
                RBase.drawable.ic_pause,
                getString(RBase.string.notif_a11y_pause_ally),
                pendingServiceIntent(MachineService.pauseAllIntent(this))
            )
        }
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1)
        )
    } else {
        val customView = RemoteViews(packageName, R.layout.layout_notif)
        customView.setTextViewText(R.id.textNotifMessage, content)
        customView.setContentDescription(
            R.id.imageNotifStopAll,
            getString(RBase.string.notif_a11y_stop_ally)
        )
        customView.setOnClickPendingIntent(
            R.id.imageNotifStopAll,
            pendingServiceIntent(MachineService.stopAllIntent(this))
        )
        if (isAllPaused) {
            customView.setImageViewResource(R.id.imageNotifPauseAll, RBase.drawable.ic_start)
            customView.setContentDescription(
                R.id.imageNotifPauseAll,
                getString(RBase.string.notif_a11y_start_ally)
            )
            customView.setOnClickPendingIntent(
                R.id.imageNotifPauseAll,
                pendingServiceIntent(MachineService.startAllIntent(this))
            )
        } else {
            customView.setImageViewResource(R.id.imageNotifPauseAll, RBase.drawable.ic_pause)
            customView.setContentDescription(
                R.id.imageNotifPauseAll,
                getString(RBase.string.notif_a11y_pause_ally)
            )
            customView.setOnClickPendingIntent(
                R.id.imageNotifPauseAll,
                pendingServiceIntent(MachineService.pauseAllIntent(this))
            )
        }
        builder.setCustomContentView(customView)
            .setCustomBigContentView(customView)
    }
    return builder
}

/**
 * Create a normal notification showing a timer's name and remaining time.
 */
internal fun Context.buildTimerNotificationBuilder(
    appNavigator: AppNavigator,
    timer: TimerEntity,
    state: StreamState,
    currentStepName: String
): Builder {
    val res = resources
    val timerId = timer.id
    val actions = mutableListOf<Action>()
    var title = ""

    val timerNamePlusStep =
        res.getString(RBase.string.notif_timer_title, timer.name, currentStepName)
    if (state.isRunning) {
        title = timerNamePlusStep
        actions.add(
            Action(
                RBase.drawable.ic_pause,
                res.getString(RBase.string.pause),
                pendingServiceIntent(MachineService.pauseTimingIntent(this, timerId), timerId)
            )
        )
        actions.add(
            Action(
                RBase.drawable.ic_plus_one,
                res.getString(RBase.string.plus_one_minute),
                pendingServiceIntent(
                    MachineService.adjustAmountIntent(this, timerId, 60_000L),
                    timerId
                )
            )
        )
    } else if (state.isPaused) {
        title = res.getString(RBase.string.notif_timer_paused, timerNamePlusStep)
        actions.add(
            Action(
                RBase.drawable.ic_start,
                res.getString(RBase.string.start),
                pendingServiceIntent(MachineService.startTimingIntent(this, timerId), timerId)
            )
        )
        actions.add(
            Action(
                RBase.drawable.ic_stop,
                res.getString(RBase.string.stop),
                pendingServiceIntent(MachineService.resetTimingIntent(this, timerId), timerId)
            )
        )
    }
    actions.add(
        Action(
            RBase.drawable.ic_arrow_down,
            res.getString(RBase.string.next),
            pendingServiceIntent(MachineService.increTimingIntent(this, timer.id), timerId)
        )
    )

    val showTimerIntent = appNavigator.getOneIntent(timerId = timerId, inNewTask = true)

    val stackBuilder = TaskStackBuilder.create(this)
    stackBuilder.addNextIntentWithParentStack(showTimerIntent)
    val pi = stackBuilder.getPendingIntent(
        timerId,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return Builder(this, CHANNEL_TIMING)
        .setShowWhen(false)
        .setSmallIcon(RBase.drawable.ic_watch)
        .setContentTitle(title)
        .setContentIntent(pi)
        .setOngoing(true)
        .setAutoCancel(false)
        .setLocalOnly(false)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSortKey("t_$timerId")
        .setColor(newDynamicTheme.colorPrimary)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .apply {
            actions.forEach { addAction(it) }
        }
        .withMediaStyleNotification(this)
        .setGroup("time")
}

private fun Builder.withMediaStyleNotification(context: Context): Builder {
    if (context.safeSharedPreference.useMediaStyleNotification) {
        val style = androidx.media.app.NotificationCompat.MediaStyle()
        @Suppress("RestrictedApi")
        when (mActions.size) {
            1 -> style.setShowActionsInCompactView(0)
            2 -> style.setShowActionsInCompactView(0, 1)
            3 -> style.setShowActionsInCompactView(0, 1, 2)
        }
        setStyle(style)
    }
    return this
}

/**
 * Create a heads-up notification to notify user.
 * Avoid filling the whole screen with our activity when the user is immersive.
 */
internal fun Context.buildScreenNotificationBuilder(
    timerItem: TimerEntity,
    currentStepName: String
): Builder {
    val res = resources
    val actions = arrayListOf<Action>()
    val title = resources.getString(RBase.string.notif_timer_screen_showing, currentStepName)
    val timerId = timerItem.id

    actions.add(
        Action(
            RBase.drawable.ic_arrow_down,
            res.getString(RBase.string.next),
            pendingServiceIntent(MachineService.increTimingIntent(this, timerId), timerId)
        )
    )
    actions.add(
        Action(
            RBase.drawable.ic_plus_one,
            res.getString(RBase.string.plus_one_minute),
            pendingServiceIntent(MachineService.adjustAmountIntent(this, timerId, 60_000L), timerId)
        )
    )

    val showScreenIntent =
        ScreenActivity.intent(this, timerId, timerItem.name, currentStepName)
    val pendingShowScreenIntent = pendingActivityIntent(showScreenIntent, timerId)

    // Full screen intent has flags so it is different than the content intent.
    val fullScreen = Intent(this, ScreenActivity::class.java)
        .putExtra(Constants.EXTRA_TIMER_ID, timerId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    val pendingFullScreen = pendingActivityIntent(fullScreen, timerId)

    return Builder(this, CHANNEL_SCREEN)
        .setShowWhen(false)
        .setSmallIcon(RBase.drawable.ic_watch)
        .setContentTitle(title)
        .setContentIntent(pendingShowScreenIntent)
        .setFullScreenIntent(pendingFullScreen, true)
        .setAutoCancel(false)
        .setOngoing(true)
        .setLocalOnly(false)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setColor(newDynamicTheme.colorPrimary)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .apply {
            actions.forEach { addAction(it) }
        }
        .withMediaStyleNotification(this)
        .setGroup("screen")
}

internal fun Context.buildBehaviourNotification(
    appNavigator: AppNavigator,
    context: Context,
    timer: TimerEntity,
    index: TimerIndex,
    duration: Int
): Builder {
    val timerId = timer.id
    val step = timer.getStep(index)
    requireNotNull(step) { "Unable to find $index in $timer" }

    val showTimerIntent = appNavigator.getOneIntent(timerId = timerId, inNewTask = true)

    val stackBuilder = TaskStackBuilder.create(this)
    stackBuilder.addNextIntentWithParentStack(showTimerIntent)
    val pi = stackBuilder.getPendingIntent(
        timerId,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return Builder(this, CHANNEL_B_NOTIF)
        .setShowWhen(true)
        .setSmallIcon(RBase.drawable.ic_notification)
        .setContentTitle(timer.name)
        .setContentText("${step.label} ${step.length.produceTime()}")
        .setContentIntent(pi)
        .setOngoing(false) // Important for Wear OS
        .setAutoCancel(true)
        .setLocalOnly(false) // Important for Wear OS
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setColor(step.type.getTypeColor(this))
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .extend(
            NotificationCompat.WearableExtender()
                .addAction(
                    Action(
                        RBase.drawable.ic_pause,
                        context.getString(RBase.string.pause),
                        pendingServiceIntent(
                            MachineService.pauseTimingIntent(this, timerId),
                            timerId
                        )
                    )
                )
                .addAction(
                    Action(
                        RBase.drawable.ic_plus_one,
                        context.getString(RBase.string.plus_one_minute),
                        pendingServiceIntent(
                            MachineService.adjustAmountIntent(this, timerId, 60_000L),
                            timerId
                        )
                    )
                )
                .addAction(
                    Action(
                        RBase.drawable.ic_stop,
                        context.getString(RBase.string.stop),
                        pendingServiceIntent(
                            MachineService.resetTimingIntent(this, timerId),
                            timerId
                        )
                    )
                )
        )
        .apply {
            if (duration > 0) {
                setTimeoutAfter(duration * 1_000L)
            }
        }
        .withMediaStyleNotification(this)
        .setGroup("notification")
}

/**
 * Update notification actions to paused state
 */
@SuppressLint("RestrictedApi")
internal fun Builder.updateStateToPaused(context: Context, id: Int): Builder = apply {
    val res = context.resources
    mActions.clear()
    addAction(
        Action(
            RBase.drawable.ic_start,
            res.getString(RBase.string.start),
            context.pendingServiceIntent(MachineService.startTimingIntent(context, id), id)
        )
    )
    addAction(
        Action(
            RBase.drawable.ic_stop,
            res.getString(RBase.string.stop),
            context.pendingServiceIntent(MachineService.resetTimingIntent(context, id), id)
        )
    )
    withMediaStyleNotification(context)
}

internal fun NotificationManager.buildChannelIfNecessary(context: Context): Unit = context.run {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (getNotificationChannel(CHANNEL_SERVICE) == null) {
            createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    getString(RBase.string.notif_channel_fore_title),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = getString(RBase.string.notif_channel_fore_desp)
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                    setSound(null, null)
                }
            )
            createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TIMING,
                    getString(RBase.string.notif_channel_timing_title),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(RBase.string.notif_channel_timing_desp)
                    enableLights(false)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(false)
                    setSound(null, null)
                }
            )
            createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SCREEN,
                    getString(RBase.string.notif_channel_screen_title),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(RBase.string.notif_channel_screen_desp)
                    enableLights(true)
                    lightColor = Color.YELLOW
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                    setSound(null, null)
                }
            )
        }
        val sp = safeSharedPreference
        val wearOsChannelKey = "migration_wear_os_channel"
        val currentChannel = getNotificationChannel(CHANNEL_B_NOTIF)
        if (currentChannel == null) {
            sp.edit { putBoolean(wearOsChannelKey, true) }
            createNotificationChannel(
                NotificationChannel(
                    CHANNEL_B_NOTIF,
                    getString(RBase.string.notif_channel_b_notif_title),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = getString(RBase.string.notif_channel_b_notif_desp)
                    enableLights(true)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                    // This line causes the notification doesn't show directly on Wear OS.
                    // setSound(null, null)
                }
            )
        } else if (!sp.getBoolean(wearOsChannelKey, false)) {
            sp.edit { putBoolean(wearOsChannelKey, true) }
            deleteNotificationChannel(CHANNEL_B_NOTIF)
            createNotificationChannel(
                currentChannel.apply {
                    setSound(
                        Settings.System.DEFAULT_NOTIFICATION_URI,
                        Notification.AUDIO_ATTRIBUTES_DEFAULT
                    )
                }
            )
        }
    }
}
