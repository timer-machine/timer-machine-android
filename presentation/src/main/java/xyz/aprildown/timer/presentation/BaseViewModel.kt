package xyz.aprildown.timer.presentation

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

abstract class SimpleViewModel : ViewModel()

abstract class BaseViewModel(
    mainDispatcher: CoroutineDispatcher
) : SimpleViewModel(), CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob() + mainDispatcher

    @CallSuper
    override fun onCleared() {
        cancel()
    }
}
