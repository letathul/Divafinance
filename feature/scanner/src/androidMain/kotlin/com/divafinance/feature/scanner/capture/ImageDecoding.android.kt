package com.divafinance.feature.scanner.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Decodes a receipt photo downsampled to [maxDimension] on its longest side and rotated the way
 * the camera actually held it.
 *
 * Both halves matter and both used to be missing from the OCR path. A 12MP photo is ~48MB of
 * ARGB_8888 — receipts are the largest images this app ever touches — and phone cameras record
 * orientation in EXIF rather than rotating the pixels, so a receipt shot in portrait arrives
 * lying on its side. Text recognition on a sideways receipt returns almost nothing usable.
 *
 * Rotation is baked into the bitmap rather than passed to ML Kit as `rotationDegrees` so that
 * the reported bounding boxes share a coordinate space with the bitmap they came from; feeding
 * the rotation separately would leave the boxes in the pre-rotation frame and silently transpose
 * every line's geometry.
 */
internal fun decodeOriented(path: String, maxDimension: Int): Bitmap? {
    if (!File(path).exists()) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    // Smallest power-of-two that brings the longest side to at most maxDimension. The previous
    // rule stopped one step early and could leave a 4000px image untouched.
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val decoded = runCatching {
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    }.getOrNull() ?: return null

    // An unreadable EXIF block is not a reason to lose the scan.
    return runCatching { applyExifOrientation(decoded, path) }.getOrDefault(decoded)
}

private fun applyExifOrientation(bitmap: Bitmap, path: String): Bitmap {
    val orientation = ExifInterface(path)
        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }

    val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (oriented != bitmap) bitmap.recycle()
    return oriented
}
