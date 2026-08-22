package com.divafinance.feature.scanner.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

/** A receipt long enough to need more than this many photographs is not a receipt. */
private const val MAX_PAGES = 10

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
            if (uri == null) {
                ImageCaptureResult.Cancelled
            } else {
                copyPagesIntoAppStorage(context, listOf(uri))
            },
        )
    }

    val scanDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val callback = pending[0]
        pending[0] = null
        // The scanner reports a cancelled scan as a non-OK result with no intent, which is
        // indistinguishable from a back press — both mean "the user changed their mind".
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            ?.pages
            ?.mapNotNull { it.imageUri }
            .orEmpty()
        callback?.invoke(
            if (pages.isEmpty()) {
                ImageCaptureResult.Cancelled
            } else {
                copyPagesIntoAppStorage(context, pages)
            },
        )
    }

    return remember(takePicture, requestCameraPermission, pickPhoto, scanDocument) {
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
                // The scanner runs in Play services' own process and asks for the camera
                // itself, so this path deliberately does not go through our permission check.
                ImageSource.DOCUMENT_SCAN -> launchDocumentScanner(
                    context = context,
                    onSender = { scanDocument.launch(IntentSenderRequest.Builder(it).build()) },
                    onFailure = { failure ->
                        pending[0] = null
                        onResult(failure)
                    },
                )
            }
        }
    }
}

@Composable
actual fun isDocumentScanSupported(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        context.findActivity() != null &&
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
}

/**
 * `SCANNER_MODE_FULL` is what buys the whole feature set in one component: edge detection,
 * automatic shutter, perspective correction, and a crop/rotate/retake editor with multi-page
 * support. Building any of that by hand would be a worse copy of something already on the
 * device.
 *
 * `getStartScanIntent` is asynchronous — Play services may have to fetch the scanner module
 * first — so the sender arrives in a listener and is handed to [onSender] rather than returned.
 * It must be launched through the Compose launcher; starting it straight off the Activity would
 * deliver the result to `onActivityResult`, where nothing is listening.
 */
private fun launchDocumentScanner(
    context: Context,
    onSender: (android.content.IntentSender) -> Unit,
    onFailure: (ImageCaptureResult) -> Unit,
) {
    val activity = context.findActivity()
    if (activity == null) {
        onFailure(ImageCaptureResult.Failed("Couldn't open the scanner"))
        return
    }
    val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(true)
        .setPageLimit(MAX_PAGES)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()
    GmsDocumentScanning.getClient(options)
        .getStartScanIntent(activity)
        .addOnSuccessListener(onSender)
        .addOnFailureListener {
            onFailure(ImageCaptureResult.Failed("Couldn't open the scanner"))
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
 * The picker's and the scanner's read grants die with the Activity while `Receipt.imagePath` is
 * a persisted column the scan history re-reads later, so the bytes have to be copied somewhere
 * durable. `core:common`'s `FileSystem` only writes text, hence doing it here.
 *
 * A partial copy is treated as a failed capture: half a multi-page receipt would parse into a
 * total the user never paid.
 */
private fun copyPagesIntoAppStorage(context: Context, uris: List<Uri>): ImageCaptureResult {
    val written = mutableListOf<File>()
    val stamp = System.currentTimeMillis()
    for ((index, uri) in uris.withIndex()) {
        val dest = File(receiptDirectory(context), "receipt-$stamp-$index.jpg")
        val copied = runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Couldn't open that image")
            stream.use { input -> dest.outputStream().use { input.copyTo(it) } }
        }.isSuccess
        if (!copied) {
            dest.delete()
            written.forEach { it.delete() }
            return ImageCaptureResult.Failed("Couldn't read that image")
        }
        written += dest
    }
    return ImageCaptureResult.Success(written.map { it.absolutePath })
}

/** `filesDir`, not `cacheDir` — an eviction would orphan every row in the scan history. */
private fun receiptDirectory(context: Context): File =
    File(context.filesDir, "receipts").apply { mkdirs() }

/** Compose hands out a themed wrapper, not the Activity the scanner needs to present from. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
