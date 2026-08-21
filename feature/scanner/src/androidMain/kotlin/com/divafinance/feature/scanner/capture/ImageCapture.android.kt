package com.divafinance.feature.scanner.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImageCaptureRequester(): ImageCaptureRequester {
    val context = LocalContext.current

    // The callbacks are held in holders because a launcher's own callback is fixed at
    // creation, while each request needs to report back to a different caller. TakePicture
    // hands back a boolean rather than a path, so the file it writes into is held too.
    val pending = remember { arrayOfNulls<(ImageCaptureResult) -> Unit>(1) }
    val pendingFile = remember { arrayOfNulls<File>(1) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        val file = pendingFile[0]
        val callback = pending[0]
        pendingFile[0] = null
        pending[0] = null
        // A cancelled capture still leaves behind the empty file we handed the camera.
        if (saved && file != null && file.length() > 0L) {
            callback?.invoke(ImageCaptureResult.Success(file.absolutePath))
        } else {
            file?.delete()
            callback?.invoke(ImageCaptureResult.Cancelled)
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCamera(context, pendingFile) { takePicture.launch(it) }
        } else {
            pending[0]?.invoke(ImageCaptureResult.Failed("Camera permission denied"))
            pending[0] = null
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val callback = pending[0]
        pending[0] = null
        callback?.invoke(
            if (uri == null) ImageCaptureResult.Cancelled else copyIntoAppStorage(context, uri),
        )
    }

    return remember(takePicture, requestCameraPermission, pickPhoto) {
        ImageCaptureRequester { source, onResult ->
            pending[0] = onResult
            when (source) {
                ImageSource.PHOTO_LIBRARY -> pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
                // ACTION_IMAGE_CAPTURE normally needs no permission, but because the app
                // *declares* CAMERA in its manifest the system requires us to hold it before
                // the capture will return a result.
                ImageSource.CAMERA -> {
                    val granted = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        launchCamera(context, pendingFile) { takePicture.launch(it) }
                    } else {
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }
}

private fun launchCamera(context: Context, slot: Array<File?>, launch: (Uri) -> Unit) {
    val file = File(receiptDirectory(context), "receipt-${System.currentTimeMillis()}.jpg")
    slot[0] = file
    // Derived at runtime rather than through a manifest placeholder, so it stays correct
    // however AGP resolves ${applicationId} for this module.
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    launch(uri)
}

/**
 * The picker's read grant dies with the Activity while `Receipt.imagePath` is a persisted
 * column the scan history re-reads later, so the bytes have to be copied somewhere durable.
 * `core:common`'s `FileSystem` only writes text, hence doing it here.
 */
private fun copyIntoAppStorage(context: Context, uri: Uri): ImageCaptureResult {
    val dest = File(receiptDirectory(context), "receipt-${System.currentTimeMillis()}.jpg")
    return runCatching {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Couldn't open that image")
        stream.use { input -> dest.outputStream().use { input.copyTo(it) } }
        ImageCaptureResult.Success(dest.absolutePath)
    }.getOrElse {
        dest.delete()
        ImageCaptureResult.Failed("Couldn't read that image")
    }
}

/** `filesDir`, not `cacheDir` — an eviction would orphan every row in the scan history. */
private fun receiptDirectory(context: Context): File =
    File(context.filesDir, "receipts").apply { mkdirs() }
