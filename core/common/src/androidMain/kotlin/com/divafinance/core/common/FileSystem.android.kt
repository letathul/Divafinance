package com.divafinance.core.common

import android.content.Context
import java.io.File

actual class FileSystem(private val context: Context) {

    actual fun getBackupDirectory(): String {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    actual fun writeText(path: String, content: String) {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
    }

    actual fun readText(path: String): String {
        return File(path).readText(Charsets.UTF_8)
    }

    actual fun listBackupFiles(): List<BackupFileInfo> {
        val dir = File(getBackupDirectory())
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension == "diva" }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                BackupFileInfo(
                    name = it.name,
                    path = it.absolutePath,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                )
            } ?: emptyList()
    }

    actual fun deleteFile(path: String): Boolean {
        return File(path).delete()
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
