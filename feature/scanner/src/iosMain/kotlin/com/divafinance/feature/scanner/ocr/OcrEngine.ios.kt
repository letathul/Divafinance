package com.divafinance.feature.scanner.ocr

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

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

                val lines = mutableListOf<String>()
                for (observation in results) {
                    val textObservation = observation as? VNRecognizedTextObservation ?: continue
                    val topCandidate = textObservation.topCandidates(1u).firstOrNull()
                    val text = (topCandidate as? platform.Vision.VNRecognizedText)?.string
                    if (text != null) {
                        lines.add(text)
                    }
                }

                cont.resume(
                    OcrResult(
                        fullText = lines.joinToString("\n"),
                        lines = lines,
                    ),
                )
            }

            request.setRecognitionLevel(VNRequestTextRecognitionLevelAccurate)
            request.setUsesLanguageCorrection(true)

            try {
                val error: NSError? = null
                requestHandler.performRequests(listOf(request), error = null)
            } catch (e: Exception) {
                cont.resumeWithException(
                    RuntimeException("Failed to perform OCR: ${e.message}"),
                )
            }
        }

    actual fun isAvailable(): Boolean = true
}
