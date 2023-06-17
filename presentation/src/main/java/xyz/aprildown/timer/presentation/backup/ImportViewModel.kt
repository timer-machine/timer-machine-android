package xyz.aprildown.timer.presentation.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.domain.usecases.data.NotifyDataChanged
import xyz.aprildown.timer.presentation.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val importAppData: ImportAppData,
    private val notifyDataChanged: NotifyDataChanged,
) : BaseViewModel(mainDispatcher) {

    private val _result: MutableLiveData<Fruit<Unit>?> = MutableLiveData()
    val result: LiveData<Fruit<Unit>?> = _result

    fun import(params: ImportAppData.Params) {
        launch {
            try {
                importAppData(params)
                notifyDataChanged()
                _result.value = Fruit.Ripe(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _result.value = Fruit.Rotten(e)
            }
        }
    }

    fun consumeResult() {
        _result.value = null
    }
}
