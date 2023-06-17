package xyz.aprildown.timer.presentation.backup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.di.MainDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ExportAppData
import xyz.aprildown.timer.presentation.BaseViewModel
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val exportAppData: ExportAppData,
) : BaseViewModel(mainDispatcher) {

    private val _result: MutableLiveData<Fruit<Unit>?> = MutableLiveData()
    val result: LiveData<Fruit<Unit>?> = _result

    fun export(params: ExportAppData.Params, outputStream: OutputStream) {
        launch {
            try {
                val data = exportAppData(params)
                withContext(ioDispatcher) {
                    outputStream.use {
                        val writer = it.bufferedWriter()
                        writer.write(data)
                        writer.flush()
                    }
                }
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
