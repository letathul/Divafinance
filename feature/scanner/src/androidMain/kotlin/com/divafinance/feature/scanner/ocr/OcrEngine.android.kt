package com.divafinance.feature.scanner.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class OcrEngine actual constructor() {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    actual suspend fun recognizeText(imagePath: String): OcrResult =
        suspendCancellableCoroutine { cont ->
            val bitmap = BitmapFactory.decodeFile(imagePath)
                ?: return@suspendCancellableCoroutine cont.resumeWithException(
                    IllegalArgumentException("Cannot decode image at: $imagePath"),
                )
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { block ->
                        block.lines.map { it.text }
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
