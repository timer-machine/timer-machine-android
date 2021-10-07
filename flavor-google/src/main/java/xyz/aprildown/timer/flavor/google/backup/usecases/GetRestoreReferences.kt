package xyz.aprildown.timer.flavor.google.backup.usecases

import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import xyz.aprildown.timer.domain.di.IoDispatcher
import xyz.aprildown.timer.domain.usecases.Fruit
import xyz.aprildown.timer.flavor.google.utils.setUpFirebaseStorage
import javax.inject.Inject

internal class GetRestoreReferences @Inject constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {

    init {
        setUpFirebaseStorage()
    }

    suspend fun get(
        reference: StorageReference
    ): Fruit<List<Pair<StorageReference, StorageMetadata>>> = withContext(dispatcher) {
        try {
            val files = reference.listAll().await()
            val items = files.items
            yield()

            val nullableResult =
                MutableList<Pair<StorageReference, StorageMetadata>?>(items.size) { null }

            coroutineScope {
                for (fileIndex in items.indices) {
                    val file = items[fileIndex]
                    launch {
                        nullableResult[fileIndex] = file to file.metadata.await()
                    }
                }
            }

            Fruit.Ripe(
                nullableResult
                    .filterNotNull()
                    .sortedByDescending { it.second.updatedTimeMillis }
            )
        } catch (e: Exception) {
            Fruit.Rotten(e)
        }
    }
}
