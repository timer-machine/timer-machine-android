package xyz.aprildown.timer.flavor.google.count

import android.content.Context
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.ktx.storage
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import net.lingala.zip4j.ZipFile
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.flavor.google.utils.setUpFirebaseStorage
import java.io.File
import java.util.Locale
import javax.inject.Inject

@Reusable
internal class LoadBakedCountResources @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context,
) : CoroutinesUseCase<Unit, Fruit<Unit>>(dispatcher) {
    override suspend fun create(params: Unit): Fruit<Unit> {
        val tempZipFile = createTempFile()

        var downloaded = false

        setUpFirebaseStorage()
        return try {
            Firebase.storage.reference
                .child(PreferenceData.BAKED_COUNT_NAME)
                .child("${Locale.getDefault().toLanguageTag()}.zip")
                .getFile(tempZipFile)
                .await()
            downloaded = true
            Fruit.Ripe(Unit)
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Firebase.storage.reference
                    .child(PreferenceData.BAKED_COUNT_NAME)
                    .child("en.zip")
                    .getFile(tempZipFile)
                    .await()
                downloaded = true
                Fruit.Ripe(Unit)
            } else {
                Fruit.Rotten(e)
            }
        } finally {
            if (downloaded) {
                unzip(tempZipFile)
            }
            tempZipFile.delete()
        }
    }

    private fun createTempFile(): File {
        val tempZipFile =
            File(applicationContext.cacheDir, "${PreferenceData.BAKED_COUNT_NAME}.zip")
        if (tempZipFile.exists()) {
            tempZipFile.delete()
        }
        tempZipFile.createNewFile()
        return tempZipFile
    }

    private fun unzip(zipFile: File) {
        val targetFolder = File(applicationContext.filesDir, PreferenceData.BAKED_COUNT_NAME)
        if (targetFolder.exists()) {
            targetFolder.deleteRecursively()
        }
        targetFolder.mkdirs()

        ZipFile(zipFile).extractAll(targetFolder.absolutePath)
    }
}
