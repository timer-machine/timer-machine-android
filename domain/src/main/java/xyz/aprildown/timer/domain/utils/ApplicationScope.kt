package xyz.aprildown.timer.domain.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private val applicationScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

fun fireAndForget(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
) {
    applicationScope.launch(context) {
        block.invoke()
    }
}
