package xyz.aprildown.timer.app.backup

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import xyz.aprildown.timer.domain.usecases.Fruit
import javax.inject.Inject

@HiltViewModel
internal class Export2ViewModel @Inject constructor() : BaseBackupViewModel() {
    override suspend fun backup(screen: Screen): Fruit<Unit> {
        delay(5000)
        return Fruit.Rotten("Hey")
    }
}
