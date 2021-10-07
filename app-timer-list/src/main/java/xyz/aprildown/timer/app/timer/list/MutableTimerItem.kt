package xyz.aprildown.timer.app.timer.list

import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo
import xyz.aprildown.timer.presentation.stream.StreamState

internal data class MutableTimerItem(
    val timerInfo: TimerInfo,
    var timerItem: TimerEntity?,
    var state: StreamState,
    var isExpanded: Boolean
) {
    val timerId = timerInfo.id
    val timerName = timerInfo.name
}
