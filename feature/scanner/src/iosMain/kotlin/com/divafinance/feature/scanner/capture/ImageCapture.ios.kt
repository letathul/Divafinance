package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGFloat
import platform.Foundation.NSData
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

@OptIn(ExperimentalForeignApi::class)
private class IosImageCaptureHolder {

    private var onResult: ((ImageCaptureResult) -> Unit)? = null
    private var cameraDelegate: CameraDelegate? = null
    private var pickerDelegate: PickerDelegate? = null

    fun request(source: ImageSource, callback: (ImageCaptureResult) -> Unit) {
        onResult = callback
        val presenter = topViewController()
        if (presenter == null) {
            finish(ImageCaptureResult.Failed("Couldn't open the photo picker"))
            return
        }
        when (source) {
            ImageSource.CAMERA -> presentCamera(presenter)
            ImageSource.PHOTO_LIBRARY -> presentLibrary(presenter)
        }
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
        finish(writeToDisk(data))
    }

    fun onData(data: NSData?) = finish(writeToDisk(data))

    fun onCancelled() = finish(ImageCaptureResult.Cancelled)

    private fun writeToDisk(data: NSData?): ImageCaptureResult {
        if (data == null) return ImageCaptureResult.Failed("Couldn't read that image")
        val directory = receiptDirectory()
            ?: return ImageCaptureResult.Failed("Couldn't save that image")
        val path = "$directory/receipt-${NSUUID().UUIDString}.jpg"
        return if (data.writeToFile(path, atomically = true)) {
            ImageCaptureResult.Success(path)
        } else {
            ImageCaptureResult.Failed("Couldn't save that image")
        }
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

// File scope rather than a companion: Kotlin/Native forbids fields on the companion of an
// ObjC-derived class, which both delegates are.
private const val PUBLIC_IMAGE = "public.image"

/** Walks past anything already presented, or the picker is attached to a hidden controller. */
private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
