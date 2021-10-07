package xyz.aprildown.timer.presentation.stream

enum class StreamState {
    RUNNING, PAUSED, RESET;

    val isRunning get() = this == RUNNING
    val isPaused get() = this == PAUSED
    val isReset get() = this == RESET
}
