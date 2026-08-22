package com.divafinance.feature.scanner.ocr

import com.divafinance.core.domain.usecase.scanner.OcrLine
import com.divafinance.feature.scanner.capture.decodeOriented
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Long side the photo is reduced to before recognition. Well above ML Kit's needs for receipt
 * type while keeping the bitmap around 12MB instead of 48MB.
 */
private const val OCR_MAX_DIMENSION = 2048

actual class OcrEngine actual constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    actual suspend fun recognizeText(imagePath: String): OcrResult =
        suspendCancellableCoroutine { cont ->
            val bitmap = decodeOriented(imagePath, OCR_MAX_DIMENSION)
                ?: return@suspendCancellableCoroutine cont.resumeWithException(
                    IllegalArgumentException("Cannot decode image at: $imagePath"),
                )
            val width = bitmap.width.toFloat()
            val height = bitmap.height.toFloat()

            // Rotation is already baked into the bitmap by decodeOriented, so the boxes below
            // come back in the same frame as the pixels.
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { line ->
                            // ML Kit reports pixels from the top-left; OcrLine wants fractions
                            // from the top-left, so this only needs scaling.
                            val box = line.boundingBox
                            if (box == null) {
                                OcrLine(line.text)
                            } else {
                                OcrLine(
                                    text = line.text,
                                    left = box.left / width,
                                    top = box.top / height,
                                    right = box.right / width,
                                    bottom = box.bottom / height,
                                )
                            }
                        }
                    }
                    cont.resume(
                        OcrResult(
                            fullText = visionText.text,
                            lines = lines,
                        ),
                    )
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    actual fun isAvailable(): Boolean = true
}
