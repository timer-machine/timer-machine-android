package xyz.aprildown.timer.presentation.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.github.deweyreed.tools.arch.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
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

    private val _error = MutableLiveData<Event<Throwable>>()
    val error: LiveData<Event<Throwable>> = _error

    fun importAppData(
        params: ImportAppData.Params,
        handlePrefs: (Map<String, String>) -> Unit,
        onSuccess: () -> Unit
    ) = launch {
        try {
            val prefs = importAppData(params)
            handlePrefs.invoke(prefs)
            notifyDataChanged()
            onSuccess.invoke()
        } catch (e: Exception) {
            _error.value = Event(e)
        }
    }
}
