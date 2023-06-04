package xyz.aprildown.timer.flavor.google.backup.usecases

import android.content.Context
import com.google.firebase.storage.StorageReference
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.domain.usecases.data.ImportAppData
import xyz.aprildown.timer.flavor.google.utils.ensureNewFile
import xyz.aprildown.timer.flavor.google.utils.setUpFirebaseStorage
import java.io.File
import javax.inject.Inject

@Reusable
class RestoreFromCloud @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context,
    private val importAppData: ImportAppData,
) {

    init {
        setUpFirebaseStorage()
    }

    suspend fun restore(reference: StorageReference): Fruit<Unit> = withContext(dispatcher) {
        try {
            val destFile = File(applicationContext.cacheDir, "file_to_restore.json").ensureNewFile()

            try {
                reference.getFile(destFile).await()

                importAppData(
                    ImportAppData.Params(
                        data = destFile.readText(),
                        wipeFirst = true,
                        importTimers = true,
                        importSchedulers = true,
                        importTimerStamps = true,
                        importPreferences = true,
                    )
                )
            } finally {
                destFile.delete()
            }
            Fruit.Ripe(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Fruit.Rotten(e)
        }
    }
}
