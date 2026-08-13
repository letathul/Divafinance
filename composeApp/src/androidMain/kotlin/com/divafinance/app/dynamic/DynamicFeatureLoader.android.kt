package com.divafinance.app.dynamic

import android.content.Context
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class DynamicFeatureLoader(context: Context) {
    private val splitInstallManager = SplitInstallManagerFactory.create(context)

    actual fun isInstalled(module: DynamicModule): Boolean {
        return splitInstallManager.installedModules.contains(module.moduleName)
    }

    actual suspend fun requestInstall(
        module: DynamicModule,
        onProgress: (Float) -> Unit,
    ): Boolean {
        if (isInstalled(module)) return true

        return suspendCancellableCoroutine { continuation ->
            val request = SplitInstallRequest.newBuilder()
                .addModule(module.moduleName)
                .build()

            val listener = SplitInstallStateUpdatedListener { state ->
                when (state.status()) {
                    SplitInstallSessionStatus.INSTALLED -> {
                        if (continuation.isActive) continuation.resume(true)
                    }
                    SplitInstallSessionStatus.DOWNLOADING -> {
                        val progress = state.bytesDownloaded().toFloat() /
                            state.totalBytesToDownload().toFloat()
                        onProgress(progress)
                    }
                    SplitInstallSessionStatus.FAILED -> {
                        if (continuation.isActive) continuation.resume(false)
                    }
                    else -> {}
                }
            }

            splitInstallManager.registerListener(listener)
            continuation.invokeOnCancellation {
                splitInstallManager.unregisterListener(listener)
            }

            splitInstallManager.startInstall(request)
                .addOnFailureListener {
                    splitInstallManager.unregisterListener(listener)
                    if (continuation.isActive) continuation.resume(false)
                }
        }
    }
}
