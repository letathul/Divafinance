package com.divafinance.feature.scanner.ocr

import com.divafinance.core.domain.usecase.scanner.OcrLine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class OcrEngine actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun recognizeText(imagePath: String): OcrResult =
        suspendCancellableCoroutine { cont ->
            val imageUrl = NSURL.fileURLWithPath(imagePath)
            val requestHandler = VNImageRequestHandler(imageUrl, mapOf<Any?, Any?>())

            val request = VNRecognizeTextRequest { request, error ->
                if (error != null) {
                    cont.resumeWithException(
                        RuntimeException("OCR failed: ${error.localizedDescription}"),
                    )
                    return@VNRecognizeTextRequest
                }

                val results = request?.results
                if (results == null) {
                    cont.resume(OcrResult(fullText = "", lines = emptyList()))
                    return@VNRecognizeTextRequest
                }

                val lines = mutableListOf<OcrLine>()
                for (observation in results) {
                    val textObservation = observation as? VNRecognizedTextObservation ?: continue
                    val topCandidate = textObservation.topCandidates(1u).firstOrNull()
                    val text = (topCandidate as? VNRecognizedText)?.string ?: continue
                    lines += textObservation.boundingBox.useContents {
                        // Vision normalises to the unit square with a BOTTOM-left origin, so y
                        // has to be flipped to match OcrLine's top-left convention. Getting this
                        // wrong reads the receipt upside down without ever failing.
                        OcrLine(
                            text = text,
                            left = origin.x.toFloat(),
                            top = (1.0 - (origin.y + size.height)).toFloat(),
                            right = (origin.x + size.width).toFloat(),
                            bottom = (1.0 - origin.y).toFloat(),
                        )
                    }
                }

                // Vision returns observations in no particular order; fullText is what gets
                // stored and read by a human, so it is assembled in reading order.
                val ordered = lines.sortedWith(compareBy({ it.top }, { it.left }))
                cont.resume(
                    OcrResult(
                        fullText = ordered.joinToString("\n") { it.text },
                        lines = lines,
                    ),
                )
            }

            request.setRecognitionLevel(VNRequestTextRecognitionLevelAccurate)
            request.setUsesLanguageCorrection(true)

            try {
                requestHandler.performRequests(listOf(request), error = null)
            } catch (e: Exception) {
                cont.resumeWithException(
                    RuntimeException("Failed to perform OCR: ${e.message}"),
                )
            }
        }

    actual fun isAvailable(): Boolean = true
}
