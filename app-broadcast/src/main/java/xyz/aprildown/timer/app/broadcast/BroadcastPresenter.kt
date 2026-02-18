package xyz.aprildown.timer.app.broadcast

import android.content.Intent
import dagger.Reusable
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfo
import xyz.aprildown.timer.domain.usecases.timer.FindTimerInfoByName
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import javax.inject.Inject

@Reusable
class BroadcastPresenter @Inject constructor(
    private val findTimerInfo: FindTimerInfo,
    private val findTimerInfoByName: FindTimerInfoByName,
    private val addTimer: AddTimer,
    private val streamMachineIntentProvider: StreamMachineIntentProvider,
) {

    suspend fun handleIntent(intent: Intent): Intent? {
        val command = intent.getStringExtra(BroadcastConstants.EXTRA_COMMAND)
            ?: return null

        return when (command) {
            BroadcastConstants.COMMAND_CREATE -> handleCreate(intent)
            BroadcastConstants.COMMAND_START -> handleStart(intent)
            BroadcastConstants.COMMAND_PAUSE -> handlePause(intent)
            BroadcastConstants.COMMAND_RESUME -> handleResume(intent)
            BroadcastConstants.COMMAND_RESET -> handleReset(intent)
            BroadcastConstants.COMMAND_DISMISS -> handleDismiss(intent)
            else -> null
        }
    }

    fun requiresRunningService(intent: Intent): Boolean {
        return when (intent.getStringExtra(BroadcastConstants.EXTRA_COMMAND)) {
            BroadcastConstants.COMMAND_PAUSE,
            BroadcastConstants.COMMAND_RESET,
            BroadcastConstants.COMMAND_DISMISS -> true
            else -> false
        }
    }

    private suspend fun handleCreate(intent: Intent): Intent? {
        val durationSeconds = intent.getLongExtra(
            BroadcastConstants.EXTRA_DURATION_SECONDS, -1L
        )
        if (durationSeconds <= 0) return null

        val name = intent.getStringExtra(BroadcastConstants.EXTRA_NAME) ?: "Timer"
        val timer = TimerFactory.createCountdownTimer(durationSeconds, name)
        val newId = addTimer(timer)
        return streamMachineIntentProvider.startIntent(newId)
    }

    private suspend fun handleStart(intent: Intent): Intent? {
        val id = resolveTimerId(intent) ?: return null
        return streamMachineIntentProvider.startIntent(id)
    }

    private suspend fun handlePause(intent: Intent): Intent? {
        val id = resolveTimerId(intent) ?: return null
        return streamMachineIntentProvider.pauseIntent(id)
    }

    private suspend fun handleResume(intent: Intent): Intent? {
        val id = resolveTimerId(intent) ?: return null
        return streamMachineIntentProvider.startIntent(id)
    }

    private suspend fun handleReset(intent: Intent): Intent? {
        val id = resolveTimerId(intent) ?: return null
        return streamMachineIntentProvider.resetIntent(id)
    }

    private suspend fun handleDismiss(intent: Intent): Intent? {
        val id = resolveTimerId(intent)
        if (id != null) {
            return streamMachineIntentProvider.resetIntent(id)
        }
        // No specific timer targeted — dismiss all
        return streamMachineIntentProvider.stopAllIntent()
    }

    private suspend fun resolveTimerId(intent: Intent): Int? {
        val id = intent.getIntExtra(
            BroadcastConstants.EXTRA_TIMER_ID, TimerEntity.NULL_ID
        )
        if (id != TimerEntity.NULL_ID) {
            return if (findTimerInfo(id) != null) id else null
        }
        val name = intent.getStringExtra(BroadcastConstants.EXTRA_TIMER_NAME)
            ?: return null
        return findTimerInfoByName(name)?.id
    }
}
