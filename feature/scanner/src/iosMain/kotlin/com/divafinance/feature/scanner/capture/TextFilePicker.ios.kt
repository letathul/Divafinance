package com.divafinance.feature.scanner.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject

@Composable
actual fun rememberTextFilePicker(): TextFilePicker {
    // UIKit holds its delegates weakly, so the holder keeps a strong reference for as long
    // as the screen is composed — otherwise the delegate is collected mid-pick and the
    // callback never fires. Same reason the image requester has one.
    val holder = remember { IosTextFilePickerHolder() }
    return remember(holder) { TextFilePicker { onResult -> holder.pick(onResult) } }
}

@OptIn(ExperimentalForeignApi::class)
private class IosTextFilePickerHolder {

    private var onResult: ((TextFileResult) -> Unit)? = null
    private var delegate: DocumentPickerDelegate? = null

    fun pick(onResult: (TextFileResult) -> Unit) {
        this.onResult = onResult
        val presenter = topViewControllerForFilePicker()
        if (presenter == null) {
            finish(TextFileResult.Failed("Couldn't open the file picker"))
            return
        }
        val types = listOf(UTTypeCommaSeparatedText, UTTypePlainText, UTTypeText)
        val controller = UIDocumentPickerViewController(forOpeningContentTypes = types)
        val pickerDelegate = DocumentPickerDelegate(::handleUrl) { finish(TextFileResult.Cancelled) }
        delegate = pickerDelegate
        controller.delegate = pickerDelegate
        presenter.presentViewController(controller, animated = true, completion = null)
    }

    private fun handleUrl(url: NSURL) {
        // A picked file lives outside the sandbox, so it has to be opened inside a
        // security-scoped access window or the read fails with a permission error.
        val scoped = url.startAccessingSecurityScopedResource()
        try {
            val content = NSString.stringWithContentsOfURL(
                url = url,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
            if (content == null) {
                finish(TextFileResult.Failed("Couldn't read that file"))
            } else {
                finish(TextFileResult.Success(url.lastPathComponent ?: "statement.csv", content))
            }
        } finally {
            if (scoped) url.stopAccessingSecurityScopedResource()
        }
    }

    private fun finish(result: TextFileResult) {
        val callback = onResult
        onResult = null
        delegate = null
        callback?.invoke(result)
    }
}

private class DocumentPickerDelegate(
    private val onPicked: (NSURL) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) onCancelled() else onPicked(url)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}

/** Walks past anything already presented, or the picker attaches to a hidden controller. */
private fun topViewControllerForFilePicker(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
