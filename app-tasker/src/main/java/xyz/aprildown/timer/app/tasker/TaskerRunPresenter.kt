package xyz.aprildown.timer.app.tasker

import android.content.Intent
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import javax.inject.Inject

internal class TaskerRunPresenter @Inject constructor(
    private val findTimerInfo: FindTimerInfo,
    private val streamMachineIntentProvider: StreamMachineIntentProvider
) {
    suspend fun isValidTimerId(timerId: Int): Boolean {
        return findTimerInfo(timerId) != null
    }

    fun start(timerId: Int): Intent = streamMachineIntentProvider.startIntent(timerId)

    fun stop(timerId: Int): Intent = streamMachineIntentProvider.resetIntent(timerId)
}
