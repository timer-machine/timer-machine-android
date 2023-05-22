package xyz.aprildown.timer.app.tasker

import android.os.Bundle
import xyz.aprildown.timer.domain.entities.TimerEntity

internal const val TASKER_TIMER_ID = "timer_id"
internal const val TASKER_ACTION = "action"
internal const val TASKER_ACTION_START = "start"
internal const val TASKER_ACTION_STOP = "stop"

internal fun Bundle.getTaskerTimerId(): Int = getInt(TASKER_TIMER_ID, TimerEntity.NULL_ID)
internal fun Bundle.getTaskerTimerAction(): String = getString(TASKER_ACTION, TASKER_ACTION_START)
