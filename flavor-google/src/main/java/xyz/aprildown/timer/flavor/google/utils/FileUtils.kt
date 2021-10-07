package xyz.aprildown.timer.flavor.google.utils

import java.io.File

internal fun File.ensureNewFile(): File {
    if (exists()) {
        delete()
    }
    createNewFile()
    return this
}
