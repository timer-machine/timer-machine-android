package xyz.aprildown.timer.app.timer.run

import android.content.Context
import androidx.core.app.NotificationCompat.Builder
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.presentation.stream.StreamState
import xyz.aprildown.timer.presentation.stream.TimerIndex
import xyz.aprildown.timer.presentation.stream.getStep
import xyz.aprildown.timer.app.base.R as RBase

internal abstract class MachineNotif(protected val context: Context) {
    lateinit var builder: Builder

    fun createStub(): Builder {
        builder = stub()
        return builder
    }

    protected abstract fun stub(): Builder

    fun start(index: TimerIndex): Builder {
        builder = withStartEvent(builder, index)
        return builder
    }

    protected abstract fun withStartEvent(b: Builder, index: TimerIndex): Builder

    fun pause(): Builder {
        builder = withPauseEvent(builder)
        return builder
    }

    protected abstract fun withPauseEvent(b: Builder): Builder

    fun update(remaining: Long): Builder {
        builder = withUpdateEvent(builder, remaining)
        return builder
    }

    protected abstract fun withUpdateEvent(b: Builder, remaining: Long): Builder
}

internal class TimerNotif(
    context: Context,
    private val appNavigator: AppNavigator,
    private val timer: TimerEntity
) : MachineNotif(context) {

    private var currentTotalLength: Long = 0L

    private val updateTimeFunc: (Builder, Long) -> Builder = if (timer.more.notifCount) { b, time ->
        b.apply {
            setContentText(time.produceTime())
            if (currentTotalLength > 0) {
                setProgress(
                    100,
                    ((time.toFloat() / currentTotalLength.toFloat() * 100).toInt()),
                    false
                )
            }
        }
    } else { b, _ -> b }

    override fun stub(): Builder {
        return context.buildTimerNotificationBuilder(
            appNavigator = appNavigator,
            timer = TimerEntity(TimerEntity.NULL_ID, "", 0, listOf()),
            state = StreamState.RESET,
            currentStepName = ""
        )
    }

    override fun withStartEvent(b: Builder, index: TimerIndex): Builder {
        currentTotalLength = timer.getStep(index)?.length ?: 0L
        return context.buildTimerNotificationBuilder(
            appNavigator = appNavigator,
            timer = timer,
            state = StreamState.RUNNING,
            currentStepName = timer.getStep(index)?.label ?: ""
        )
    }

    override fun withPauseEvent(b: Builder): Builder {
        return builder.apply {
            setContentTitle(context.getString(RBase.string.notif_timer_paused, timer.name))
            updateStateToPaused(context, timer.id)
        }
    }

    override fun withUpdateEvent(b: Builder, remaining: Long): Builder {
        return updateTimeFunc.invoke(b, remaining)
    }
}
