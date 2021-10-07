package xyz.aprildown.timer.flavor.google.count

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.usecases.CoroutinesUseCase
import java.io.File
import javax.inject.Inject

@Reusable
internal class ClearBakedCountResources @Inject constructor(
    @IoDispatcher dispatcher: CoroutineDispatcher,
    @ApplicationContext private val applicationContext: Context,
) : CoroutinesUseCase<Unit, Unit>(dispatcher) {
    override suspend fun create(params: Unit) {
        val targetFolder = File(applicationContext.filesDir, PreferenceData.BAKED_COUNT_NAME)
        if (targetFolder.exists()) {
            targetFolder.deleteRecursively()
        }
    }
}
