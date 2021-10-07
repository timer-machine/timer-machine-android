package xyz.aprildown.timer.domain.usecases

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

abstract class CoroutinesUseCase<in Params, Result>(protected val dispatcher: CoroutineDispatcher) {

    protected abstract suspend fun create(params: Params): Result

    suspend fun execute(params: Params): Result = withContext(dispatcher) {
        create(params)
    }

    suspend operator fun invoke(params: Params): Result = execute(params)
}

suspend operator fun <Result> CoroutinesUseCase<Unit, Result>.invoke(): Result = invoke(Unit)
