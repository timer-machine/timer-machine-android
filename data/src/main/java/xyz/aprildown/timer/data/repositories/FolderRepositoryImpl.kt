package xyz.aprildown.timer.data.repositories

import dagger.Reusable
import xyz.aprildown.timer.data.db.FolderDao
import xyz.aprildown.timer.data.mappers.FolderMapper
import xyz.aprildown.timer.data.mappers.fromWithMapper
import xyz.aprildown.timer.data.mappers.toWithMapper
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.repositories.FolderRepository
import javax.inject.Inject

@Reusable
internal class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val folderMapper: FolderMapper
) : FolderRepository {

    override suspend fun getFolders(): List<FolderEntity> {
        return folderDao.getFolders().fromWithMapper(folderMapper)
    }

    override suspend fun addFolder(item: FolderEntity): Long {
        return folderDao.addFolder(item.toWithMapper(folderMapper))
    }

    override suspend fun updateFolder(item: FolderEntity): Boolean {
        return folderDao.updateFolder(item.toWithMapper(folderMapper)) == 1
    }

    override suspend fun deleteFolder(id: Long) {
        folderDao.deleteFolder(id)
    }
}
