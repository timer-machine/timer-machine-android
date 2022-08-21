package xyz.aprildown.timer.domain.usecases.home

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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

    private val tipLiveData: MutableLiveData<Int> = MutableLiveData()

    fun getTipLiveData(coroutineScope: CoroutineScope): LiveData<Int> {
        return tipLiveData.also {
            coroutineScope.launch(dispatcher) {
                if (!AppConfig.showFirstTimeInfo) return@launch

                val isOldUser = repo.contains(PREF_OLD_INTRO)
                if (!repo.contains(TIP_TUTORIAL.tipKey) && !isOldUser) {
                    tipLiveData.postValue(TIP_TUTORIAL)
                } else if (!repo.contains(TIP_WHITELIST.tipKey) && !isOldUser) {
                    tipLiveData.postValue(TIP_WHITELIST)
                } else {
                    if (!repo.getBoolean(TIP_MISSED_TIMER.tipKey, true) &&
                        !appTracker.hasCrashedInLastSession()
                    ) {
                        tipLiveData.postValue(TIP_MISSED_TIMER)
                    }
                }
            }
        }
    }

    suspend fun consumeTip(tip: Int) = withContext(dispatcher) {
        repo.setBoolean(tip.tipKey, true)

        when (tip) {
            TIP_TUTORIAL -> tipLiveData.postValue(TIP_WHITELIST)
            TIP_WHITELIST, TIP_MISSED_TIMER -> tipLiveData.postValue(TIP_NO_MORE)
            else -> error("Unknown onboarding tip $tip")
        }
    }

    suspend fun setTip(tip: Int) = withContext(dispatcher) {
        repo.setBoolean(tip.tipKey, false)
    }

    companion object {

        private val Int.tipKey: String get() = "home_tip$this"

        const val TIP_NO_MORE = 0
        const val TIP_TUTORIAL = 1
        const val TIP_WHITELIST = 2
        const val TIP_MISSED_TIMER = 3

        private const val PREF_OLD_INTRO = "pref_first_start_app_screen"

        fun hasTutorialViewed(sharedPreferences: SharedPreferences): Boolean {
            if (sharedPreferences.contains(PREF_OLD_INTRO)) return true
            if (sharedPreferences.contains(TIP_TUTORIAL.tipKey)) return true
            return false
        }
    }
}
