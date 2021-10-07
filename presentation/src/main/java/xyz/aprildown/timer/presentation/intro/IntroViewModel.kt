package xyz.aprildown.timer.presentation.intro

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.usecases.timer.AddTimer
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val addTimer: AddTimer,
) : BaseViewModel(mainDispatcher) {

    private val addedTimers = mutableListOf<TimerEntity>()

    fun addSampleTimer(timer: TimerEntity) = launch {
        if (timer in addedTimers) return@launch

        addTimer(timer)

        addedTimers += timer
    }
}
