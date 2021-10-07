package xyz.aprildown.timer.domain.entities

data class FolderEntity(
    val id: Long,
    val name: String
) {

    val isDefault: Boolean get() = id == FOLDER_DEFAULT
    val isTrash: Boolean get() = id == FOLDER_TRASH

    companion object {
        const val NEW_ID = 0L

        const val FOLDER_DEFAULT = 1L // Long.MAX_VALUE is the max value of SQLite.
        const val FOLDER_TRASH = 2L
    }
}

enum class FolderSortBy {
    AddedNewest, AddedOldest, RunNewest, RunOldest, AToZ, ZToA,
}
