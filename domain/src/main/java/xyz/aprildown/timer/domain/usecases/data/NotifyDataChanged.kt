package xyz.aprildown.timer.domain.usecases.data

import dagger.Reusable
import xyz.aprildown.timer.domain.repositories.AppDataRepository
import xyz.aprildown.timer.domain.utils.fireAndForget
import javax.inject.Inject

@Reusable
class NotifyDataChanged @Inject constructor(
    private val repo: AppDataRepository
) {
    operator fun invoke() {
        fireAndForget {
            repo.notifyDataChanged()
        }
    }
}
