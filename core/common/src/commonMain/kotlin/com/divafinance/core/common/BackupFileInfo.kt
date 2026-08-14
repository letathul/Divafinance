package com.divafinance.core.common

data class BackupFileInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
)
