package xyz.aprildown.timer.flavor.google.count

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import com.github.deweyreed.tools.arch.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.app.base.data.PreferenceData.useBakedCount
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.invoke
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class BakedCountViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val sharedPreferences: SharedPreferences,
    private val loadBakedCountResources: LoadBakedCountResources,
    private val clearBakedCountResources: ClearBakedCountResources,
) : BaseViewModel(mainDispatcher) {

    private val _state = MutableLiveData<Int>()
    val state: LiveData<Int> = _state.distinctUntilChanged()

    private val _error = MutableLiveData<Event<Throwable>>()
    val error: LiveData<Event<Throwable>> = _error

    var shouldShowPromotionDialog = true

    fun init() {
        shouldShowPromotionDialog = true
        _error.value?.getContentIfNotHandled()
        if (sharedPreferences.useBakedCount) {
            _state.value = STATE_ENABLED
        } else {
            _state.value = STATE_LOADING_PRO_STATE
        }
    }

    fun proStateUpdated(hasPro: Boolean) {
        if (_state.value == STATE_ENABLED) return
        _state.value = if (hasPro) STATE_DISABLED else STATE_TO_BE_PURCHASED
    }

    fun download() {
        if (_state.value == STATE_ENABLED) return

        val originalValue = _state.value ?: return
        _state.value = STATE_DOWNLOADING

        launch {
            when (val fruit = loadBakedCountResources()) {
                is Fruit.Ripe -> {
                    _state.value = STATE_ENABLED
                    sharedPreferences.useBakedCount = true
                }
                is Fruit.Rotten -> {
                    _state.value = originalValue
                    _error.value = Event(fruit.exception)
                }
            }
        }
    }

    fun disable() {
        if (_state.value == STATE_ENABLED) {
            _state.value = STATE_LOADING_PRO_STATE
            launch {
                clearBakedCountResources()
                sharedPreferences.useBakedCount = false
            }
        }
    }

    companion object {
        const val STATE_ENABLED = 0
        const val STATE_LOADING_PRO_STATE = 1
        const val STATE_DISABLED = 2
        const val STATE_TO_BE_PURCHASED = 3
        const val STATE_DOWNLOADING = 4
    }
}
