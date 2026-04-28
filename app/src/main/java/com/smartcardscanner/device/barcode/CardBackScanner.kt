package com.smartcardscanner.device.barcode

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.smartcardscanner.domain.model.BarcodeData
import com.smartcardscanner.domain.model.MrzData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CardBackScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mrzParser: MrzParser,
    private val payloadParser: BarcodePayloadParser = BarcodePayloadParser()
) {
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val TAG = "CardBackScanner"
    }

    data class ScanResult(
        val mrzData: MrzData? = null,
        val barcodeData: BarcodeData? = null,
        val ocrText: String = "",
        val nameEnglish: String = "",
        val nameArabic: String = "",
        val documentNumber: String = "",
        val nationalId: String = "",
        val dateOfBirth: String = "",
        val placeOfBirth: String = "",
        val governorate: String = "",
        val district: String = "",
        val motherName: String = "",
        val isSuccess: Boolean = false
    )

    @OptIn(ExperimentalGetImage::class)
    suspend fun processImage(imageProxy: ImageProxy): ScanResult {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return ScanResult()
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // Run barcode and text recognition in parallel
        val barcodeResult = scanBarcode(image)
        val textResult = scanText(image)

        imageProxy.close()

        // Parse MRZ from text
        val mrzLines = mrzParser.extractMrzFromText(textResult)
        val mrzData = mrzLines?.let { mrzParser.parseTD1(it) }

        // Extract name from text (English name line) — fallback only.
        val nameEnglishFromOcr = extractEnglishName(textResult)

        // Prefer the structured PDF417 payload when available — that's where
        // the Arabic name lives on Yemeni eID cards.
        val nameEnglish = barcodeResult?.nameEnglish?.takeIf { it.isNotBlank() }
            ?: nameEnglishFromOcr
            ?: mrzData?.fullNameEnglish
            ?: ""
        val nameArabic = barcodeResult?.nameArabic.orEmpty()
        val nationalId = barcodeResult?.nationalId.orEmpty()
        val dateOfBirth = barcodeResult?.dateOfBirth.orEmpty()
        val extra = barcodeResult?.additionalData ?: emptyMap()

        val isSuccess = mrzData != null || barcodeResult != null

        return ScanResult(
            mrzData = mrzData,
            barcodeData = barcodeResult,
            ocrText = textResult,
            nameEnglish = nameEnglish,
            nameArabic = nameArabic,
            documentNumber = mrzData?.documentNumber ?: "",
            nationalId = nationalId,
            dateOfBirth = dateOfBirth,
            placeOfBirth = extra["placeOfBirth"].orEmpty(),
            governorate = extra["governorate"].orEmpty(),
            district = extra["district"].orEmpty(),
            motherName = extra["motherName"].orEmpty(),
            isSuccess = isSuccess
        )
    }

    private suspend fun scanBarcode(image: InputImage): BarcodeData? {
        return suspendCancellableCoroutine { cont ->
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    if (barcode != null) {
                        val raw = barcode.rawValue ?: ""
                        val parsed = payloadParser.parse(raw).copy(
                            format = formatName(barcode.format)
                        )
                        cont.resume(parsed)
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scanning failed", e)
                    cont.resume(null)
                }
        }
    }

    private suspend fun scanText(image: InputImage): String {
        return suspendCancellableCoroutine { cont ->
            textRecognizer.process(image)
                .addOnSuccessListener { text ->
                    cont.resume(text.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                    cont.resume("")
                }
        }
    }

    private fun extractEnglishName(text: String): String? {
        // Look for "Name:" line
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Name:", ignoreCase = true) ||
                trimmed.startsWith("Name :", ignoreCase = true)) {
                return trimmed.substringAfter(":").trim()
            }
        }
        return null
    }

    private fun formatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_DATA_MATRIX -> "DataMatrix"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Unknown ($format)"
        }
    }
}
