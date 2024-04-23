package xyz.aprildown.timer.domain.utils

import java.io.File

fun File.ensureDirExistence(): File {
    if (!exists()) mkdirs()
    return this
}

fun File.ensureNewFile(): File {
    parentFile?.ensureDirExistence()
    if (exists()) delete()
    createNewFile()
    return this
}
