package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationController
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.VisionKit.VNDocumentCameraScan
import platform.VisionKit.VNDocumentCameraViewController
import platform.VisionKit.VNDocumentCameraViewControllerDelegateProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberImageCaptureRequester(): ImageCaptureRequester {
    // UIKit holds its delegates weakly, so the holder keeps a strong reference for as long
    // as the screen is composed — otherwise the delegate is collected mid-pick and the
    // callback never fires.
    val holder = remember { IosImageCaptureHolder() }
    return remember(holder) {
        ImageCaptureRequester { source, onResult ->
            holder.request(source, onResult)
        }
    }
}

@Composable
actual fun isDocumentScanSupported(): Boolean =
    remember { VNDocumentCameraViewController.isSupported() }

@OptIn(ExperimentalForeignApi::class)
private class IosImageCaptureHolder {

    private var onResult: ((ImageCaptureResult) -> Unit)? = null
    private var cameraDelegate: CameraDelegate? = null
    private var pickerDelegate: PickerDelegate? = null
    private var scannerDelegate: ScannerDelegate? = null

    fun request(source: ImageSource, callback: (ImageCaptureResult) -> Unit) {
        onResult = callback
        val presenter = topViewController()
        if (presenter == null) {
            finish(ImageCaptureResult.Failed("Couldn't open the photo picker"))
            return
        }
        when (source) {
            ImageSource.DOCUMENT_SCAN -> presentScanner(presenter)
            ImageSource.CAMERA -> presentCamera(presenter)
            ImageSource.PHOTO_LIBRARY -> presentLibrary(presenter)
        }
    }

    /**
     * VisionKit's own scanner: it finds the page edges, fires the shutter when the receipt is
     * square in frame, corrects the perspective and lets the user retake or crop — the same
     * feature set the Android side gets from ML Kit, and multi-page for free.
     */
    private fun presentScanner(presenter: UIViewController) {
        if (!VNDocumentCameraViewController.isSupported()) {
            // No camera, so no scanner; the simulator lands here.
            finish(ImageCaptureResult.Failed("Document scanning isn't available on this device"))
            return
        }
        val controller = VNDocumentCameraViewController()
        val delegate = ScannerDelegate(this)
        scannerDelegate = delegate
        controller.delegate = delegate
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    private fun presentCamera(presenter: UIViewController) {
        val sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
            // The simulator has no camera; the library still works there.
            finish(ImageCaptureResult.Failed("This device has no camera"))
            return
        }
        val controller = UIImagePickerController()
        controller.sourceType = sourceType
        val delegate = CameraDelegate(this)
        cameraDelegate = delegate
        controller.delegate = delegate
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    private fun presentLibrary(presenter: UIViewController) {
        val configuration = PHPickerConfiguration()
        configuration.filter = PHPickerFilter.imagesFilter()
        configuration.selectionLimit = 1
        val controller = PHPickerViewController(configuration)
        val delegate = PickerDelegate(this)
        pickerDelegate = delegate
        controller.delegate = delegate
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    fun onImage(image: UIImage?) {
        val data = image?.let { UIImageJPEGRepresentation(it, JPEG_QUALITY) }
        finish(writeToDisk(listOfNotNull(data)))
    }

    fun onData(data: NSData?) = finish(writeToDisk(listOfNotNull(data)))

    fun onPages(images: List<UIImage>) {
        finish(writeToDisk(images.mapNotNull { UIImageJPEGRepresentation(it, JPEG_QUALITY) }))
    }

    fun onCancelled() = finish(ImageCaptureResult.Cancelled)

    fun onFailed(message: String) = finish(ImageCaptureResult.Failed(message))

    /**
     * All or nothing: half a multi-page receipt would parse into a total the user never paid,
     * so a page that fails to write discards the ones already written with it.
     */
    private fun writeToDisk(pages: List<NSData>): ImageCaptureResult {
        if (pages.isEmpty()) return ImageCaptureResult.Failed("Couldn't read that image")
        val directory = receiptDirectory()
            ?: return ImageCaptureResult.Failed("Couldn't save that image")

        val written = mutableListOf<String>()
        for (data in pages) {
            val path = "$directory/receipt-${NSUUID().UUIDString}.jpg"
            if (!data.writeToFile(path, atomically = true)) {
                written.forEach {
                    NSFileManager.defaultManager.removeItemAtPath(it, error = null)
                }
                return ImageCaptureResult.Failed("Couldn't save that image")
            }
            written += path
        }
        return ImageCaptureResult.Success(written)
    }

    /**
     * Picker and item-provider callbacks arrive off the main thread, so the hop back is not
     * optional — the ViewModel writes state from here.
     */
    private fun finish(result: ImageCaptureResult) {
        val callback = onResult
        onResult = null
        cameraDelegate = null
        pickerDelegate = null
        scannerDelegate = null
        dispatch_async(dispatch_get_main_queue()) { callback?.invoke(result) }
    }

    private fun receiptDirectory(): String? {
        val documents = NSFileManager.defaultManager.URLsForDirectory(
            NSDocumentDirectory,
            NSUserDomainMask,
        ).firstOrNull() as? NSURL ?: return null
        val directory = documents.URLByAppendingPathComponent("receipts")?.path ?: return null
        NSFileManager.defaultManager.createDirectoryAtPath(
            directory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        return directory
    }

    private companion object {
        const val JPEG_QUALITY: Double = 0.9
    }
}

@OptIn(ExperimentalForeignApi::class)
private class CameraDelegate(
    private val holder: IosImageCaptureHolder,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true) { holder.onImage(image) }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true) { holder.onCancelled() }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PickerDelegate(
    private val holder: IosImageCaptureHolder,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        picker.dismissViewControllerAnimated(true, completion = null)
        if (result == null) {
            holder.onCancelled()
            return
        }
        result.itemProvider.loadDataRepresentationForTypeIdentifier(PUBLIC_IMAGE) { data, _ ->
            holder.onData(data)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class ScannerDelegate(
    private val holder: IosImageCaptureHolder,
) : NSObject(), VNDocumentCameraViewControllerDelegateProtocol {

    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFinishWithScan: VNDocumentCameraScan,
    ) {
        // The scan object is only valid until the controller goes away, so the pages are
        // pulled out before dismissing rather than in the completion block.
        val pages = (0uL until didFinishWithScan.pageCount).mapNotNull {
            didFinishWithScan.imageOfPageAtIndex(it)
        }
        controller.dismissViewControllerAnimated(true) { holder.onPages(pages) }
    }

    override fun documentCameraViewControllerDidCancel(
        controller: VNDocumentCameraViewController,
    ) {
        controller.dismissViewControllerAnimated(true) { holder.onCancelled() }
    }

    override fun documentCameraViewController(
        controller: VNDocumentCameraViewController,
        didFailWithError: NSError,
    ) {
        controller.dismissViewControllerAnimated(true) {
            holder.onFailed(didFailWithError.localizedDescription)
        }
    }
}

// File scope rather than a companion: Kotlin/Native forbids fields on the companion of an
// ObjC-derived class, which all three delegates are.
private const val PUBLIC_IMAGE = "public.image"

/** Walks past anything already presented, or the picker is attached to a hidden controller. */
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
