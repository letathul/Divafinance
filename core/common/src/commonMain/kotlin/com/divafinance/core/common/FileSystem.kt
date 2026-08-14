package com.divafinance.core.common

expect class FileSystem {
    fun getBackupDirectory(): String
    fun writeText(path: String, content: String)
    fun readText(path: String): String
    fun listBackupFiles(): List<BackupFileInfo>
    fun deleteFile(path: String): Boolean
    fun fileExists(path: String): Boolean
}
