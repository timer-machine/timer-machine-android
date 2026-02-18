package xyz.aprildown.timer.app.broadcast

internal object BroadcastConstants {
    const val ACTION_TIMER_CONTROL = "io.github.deweyreed.timer.BROADCAST_TIMER_CONTROL"

    const val EXTRA_COMMAND = "command"
    const val EXTRA_TIMER_ID = "timer_id"
    const val EXTRA_TIMER_NAME = "timer_name"
    const val EXTRA_DURATION_SECONDS = "duration_seconds"
    const val EXTRA_NAME = "name"

    const val COMMAND_CREATE = "create"
    const val COMMAND_START = "start"
    const val COMMAND_PAUSE = "pause"
    const val COMMAND_RESUME = "resume"
    const val COMMAND_RESET = "reset"
    const val COMMAND_DISMISS = "dismiss"
}
