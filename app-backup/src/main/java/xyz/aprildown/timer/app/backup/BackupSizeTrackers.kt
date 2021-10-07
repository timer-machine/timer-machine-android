package xyz.aprildown.timer.app.backup

import xyz.aprildown.timer.domain.utils.AppTracker

internal fun trackImportFileSize(appTracker: AppTracker, size: Long) {
    appTracker.trackEvent(
        event = "Restore",
        property = "Size",
        value = formatSize(size)
    )
}

internal fun trackExportFileSize(appTracker: AppTracker, size: Long) {
    appTracker.trackEvent(
        event = "Backup",
        property = "Size",
        value = formatSize(size)
    )
}

private fun formatSize(size: Long): String {
    val sizeInKb = size / 1024f
    return when {
        sizeInKb <= 10 -> "(0KB, 10KB]"
        sizeInKb <= 50 -> "(10KB, 50KB]"
        sizeInKb <= 100 -> "(50KB, 100KB]"
        sizeInKb <= 500 -> "(100KB, 500KB]"
        sizeInKb <= 1024 -> "(500KB, 1MB]"
        sizeInKb <= 1024 * 5 -> "(1MB, 5MB]"
        sizeInKb <= 1024 * 10 -> "(5MB, 10MB]"
        sizeInKb <= 1024 * 100 -> "(10MB, 100MB]"
        else -> "(100MB, +8)"
    }
}
