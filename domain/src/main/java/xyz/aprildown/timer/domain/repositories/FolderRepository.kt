package xyz.aprildown.timer.domain.repositories

import xyz.aprildown.timer.domain.entities.FolderEntity

interface FolderRepository {
    suspend fun getFolders(): List<FolderEntity>

    suspend fun addFolder(item: FolderEntity): Long

    suspend fun updateFolder(item: FolderEntity): Boolean

    suspend fun deleteFolder(id: Long)
}
