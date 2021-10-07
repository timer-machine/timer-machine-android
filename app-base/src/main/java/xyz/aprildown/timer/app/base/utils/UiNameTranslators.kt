package xyz.aprildown.timer.app.base.utils

import android.content.Context
import xyz.aprildown.timer.app.base.R
import xyz.aprildown.timer.domain.entities.FolderEntity

fun FolderEntity.getDisplayName(context: Context): String {
    return when {
        isDefault -> context.getString(R.string.folder_default)
        isTrash -> context.getString(R.string.folder_trash)
        else -> name
    }
}
