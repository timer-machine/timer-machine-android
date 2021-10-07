package xyz.aprildown.timer.presentation.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import xyz.aprildown.timer.presentation.BaseViewModel
import xyz.aprildown.tools.arch.Event
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    private val exportAppData: ExportAppData,
) : BaseViewModel(mainDispatcher) {

    private val _error = MutableLiveData<Event<Throwable>>()
    val error: LiveData<Event<Throwable>> = _error

    fun exportAppData(
        params: ExportAppData.Params,
        onExport: (String) -> Unit,
        onSuccess: () -> Unit
    ) = launch {
        try {
            val appContent = exportAppData(params)
            onExport.invoke(appContent)
            onSuccess.invoke()
        } catch (e: Exception) {
            _error.value = Event(e)
        }
    }
}
