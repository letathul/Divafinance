package com.divafinance.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.Foundation.stringWithContentsOfFile
// `timeIntervalSince1970` is declared in the NSExtendedDate category, which
// cinterop exposes as an extension property rather than an NSDate member.
import platform.Foundation.timeIntervalSince1970

// The NSFileManager/NSString APIs below all take an `error:` out-parameter,
// which cinterop models as a CPointer — hence the foreign-API opt-in.
@OptIn(ExperimentalForeignApi::class)
actual class FileSystem {
    private val fileManager = NSFileManager.defaultManager

    actual fun getBackupDirectory(): String {
        val urls = fileManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsUrl = urls.firstOrNull() as? NSURL
            ?: throw IllegalStateException("Cannot find Documents directory")
        val backupPath = documentsUrl.path + "/backups"
        if (!fileManager.fileExistsAtPath(backupPath)) {
            fileManager.createDirectoryAtPath(
                backupPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        return backupPath
    }

    actual fun writeText(path: String, content: String) {
        val parentPath = path.substringBeforeLast("/")
        if (!fileManager.fileExistsAtPath(parentPath)) {
            fileManager.createDirectoryAtPath(
                parentPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        val nsString = NSString.create(string = content)
        nsString.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }

    actual fun readText(path: String): String {
        return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
            ?: throw IllegalStateException("Cannot read file: $path")
    }

    actual fun listBackupFiles(): List<BackupFileInfo> {
        val backupDir = getBackupDirectory()
        val contents = fileManager.contentsOfDirectoryAtPath(backupDir, error = null)
            ?: return emptyList()

        return contents
            .mapNotNull { it as? String }
            .filter { it.endsWith(".diva") }
            .mapNotNull { fileName ->
                val filePath = "$backupDir/$fileName"
                val attrs = fileManager.attributesOfItemAtPath(filePath, error = null)
                    ?: return@mapNotNull null
                // Values come out of an NSDictionary, so depending on how the
                // interop bridge unwraps them they may arrive as NSNumber or as a
                // Kotlin number — accept both rather than silently defaulting to 0.
                val size = when (val raw = attrs["NSFileSize"]) {
                    is NSNumber -> raw.longLongValue
                    is Number -> raw.toLong()
                    else -> 0L
                }
                val modified = (attrs["NSFileModificationDate"] as? NSDate)
                    ?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0L
                BackupFileInfo(
                    name = fileName,
                    path = filePath,
                    sizeBytes = size,
                    lastModified = modified,
                )
            }
            .sortedByDescending { it.lastModified }
    }

    actual fun deleteFile(path: String): Boolean {
        return fileManager.removeItemAtPath(path, error = null)
    }

    actual fun fileExists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }
}
