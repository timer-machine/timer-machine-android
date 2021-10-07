package xyz.aprildown.timer.domain.usecases.data

import dagger.Reusable
import kotlinx.coroutines.runBlocking
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import javax.inject.Inject

@Reusable
class NotifyDataChanged @Inject constructor(
    private val repo: AppDataRepository
) {
    operator fun invoke() {
        runBlocking {
            repo.notifyDataChanged()
        }
    }
}
