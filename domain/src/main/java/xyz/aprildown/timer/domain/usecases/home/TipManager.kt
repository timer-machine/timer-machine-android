package xyz.aprildown.timer.domain.usecases.home

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.repositories.PreferencesRepository
import xyz.aprildown.timer.domain.utils.AppConfig
import xyz.aprildown.timer.domain.utils.AppTracker
import javax.inject.Inject

/**
 * The tip is true if it has been read. Unread otherwise.
 */
@Reusable
class TipManager @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val repo: PreferencesRepository,
    private val appTracker: AppTracker,
) {

    private val tipFlow: MutableStateFlow<Int> = MutableStateFlow(TIP_NO_MORE)

    fun getTipFlow(coroutineScope: CoroutineScope): Flow<Int> {
        coroutineScope.launch(dispatcher) {
            if (!AppConfig.showFirstTimeInfo) return@launch

            val isOldUser = repo.contains(PREF_OLD_INTRO)
            when {
                !isOldUser && !isTipChecked(TIP_TUTORIAL) -> {
                    tipFlow.value = TIP_TUTORIAL
                }
                !isOldUser && !isTipChecked(TIP_WHITELIST) -> {
                    tipFlow.value = TIP_WHITELIST
                }
                !isTipChecked(TIP_MISSED_TIMER, default = true) &&
                    !appTracker.hasCrashedInLastSession() -> {
                    tipFlow.value = TIP_MISSED_TIMER
                }
            }
        }
        return tipFlow
    }

    suspend fun consumeTip(tip: Int) = withContext(dispatcher) {
        checkTip(tip)

        when (tip) {
            TIP_TUTORIAL -> tipFlow.value = TIP_WHITELIST
            TIP_WHITELIST, TIP_MISSED_TIMER -> tipFlow.value = TIP_NO_MORE
            else -> error("Unknown onboarding tip $tip")
        }
    }

    suspend fun setTip(tip: Int) {
        checkTip(tip, false)
    }

    private suspend fun isTipChecked(tip: Int, default: Boolean = false): Boolean {
        return withContext(dispatcher) {
            val key = tip.tipKey
            if (default) {
                repo.getBoolean(key, true)
            } else {
                repo.contains(key) && repo.getBoolean(key, false)
            }
        }
    }

    private suspend fun checkTip(tip: Int, checked: Boolean = true) {
        withContext(dispatcher) {
            repo.setBoolean(tip.tipKey, checked)
        }
    }

    companion object {

        private val Int.tipKey: String get() = "home_tip$this"

        const val TIP_NO_MORE = 0
        const val TIP_TUTORIAL = 1
        const val TIP_WHITELIST = 2
        const val TIP_MISSED_TIMER = 3
        // const val TIP_ANDROID_12 = 4 // Legacy

        private const val PREF_OLD_INTRO = "pref_first_start_app_screen"
    }
}
