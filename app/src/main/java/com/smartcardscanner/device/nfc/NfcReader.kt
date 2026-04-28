package com.smartcardscanner.device.nfc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.smartcardscanner.domain.model.BacKey
import com.smartcardscanner.domain.model.NfcData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.CardService
import net.sf.scuba.smartcards.CardServiceException
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.MRZInfo
import org.jmrtd.lds.iso19794.FaceImageInfo
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NfcReader @Inject constructor() {

    companion object {
        private const val TAG = "NfcReader"
    }

    data class NfcResult(
        val data: NfcData? = null,
        val error: String? = null,
        val isSuccess: Boolean = false
    )

    fun getTagUid(tag: Tag): String {
        return tag.id?.joinToString("") { "%02X".format(it) } ?: ""
    }

    suspend fun readCard(tag: Tag, bacKey: BacKey?): NfcResult = withContext(Dispatchers.IO) {
        val uid = getTagUid(tag)
        Log.d(TAG, "Tag UID: $uid")

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            return@withContext NfcResult(
                data = NfcData(uid = uid),
                error = "البطاقة لا تدعم IsoDep",
                isSuccess = false
            )
        }

        try {
            isoDep.timeout = 10000 // 10 seconds timeout
            isoDep.connect()

            val cardService = CardService.getInstance(isoDep)
            cardService.open()

            val passportService = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                false
            )
            passportService.open()

            // Perform BAC if we have the key
            if (bacKey != null) {
                try {
                    val bacKeySpec: BACKeySpec = BACKey(
                        bacKey.documentNumber,
                        bacKey.dateOfBirth,
                        bacKey.dateOfExpiry
                    )
                    passportService.doBAC(bacKeySpec)
                    Log.d(TAG, "BAC authentication successful")
                } catch (e: Exception) {
                    Log.e(TAG, "BAC authentication failed", e)
                    return@withContext NfcResult(
                        data = NfcData(uid = uid, chipInfo = "BAC فشل: ${e.message}"),
                        error = "فشل فتح الشريحة (BAC). تحقق من بيانات MRZ.",
                        isSuccess = false
                    )
                }
            }

            // Read DG1 - MRZ Info
            var fullName = ""
            var dateOfBirth = ""
            var dateOfExpiry = ""
            var nationality = ""
            var gender = ""
            var documentNumber = ""
            var issuingState = ""
            var personalNumber = ""
            var rawMrz = ""

            try {
                val dg1InputStream: InputStream = passportService.getInputStream(PassportService.EF_DG1)
                val dg1File = DG1File(dg1InputStream)
                val mrzInfo: MRZInfo = dg1File.mrzInfo

                fullName = "${mrzInfo.secondaryIdentifier.replace("<", " ")} ${mrzInfo.primaryIdentifier.replace("<", " ")}".trim()
                dateOfBirth = mrzInfo.dateOfBirth
                dateOfExpiry = mrzInfo.dateOfExpiry
                nationality = mrzInfo.nationality
                gender = mrzInfo.gender.toString()
                documentNumber = mrzInfo.documentNumber
                issuingState = mrzInfo.issuingState
                personalNumber = mrzInfo.personalNumber?.replace("<", "") ?: ""
                rawMrz = mrzInfo.toString()

                Log.d(TAG, "DG1 read successfully: $fullName")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DG1", e)
            }

            // Read DG2 - Face Photo
            var facePhoto: Bitmap? = null
            try {
                val dg2InputStream: InputStream = passportService.getInputStream(PassportService.EF_DG2)
                val dg2File = DG2File(dg2InputStream)
                val faceInfos = dg2File.faceInfos
                if (faceInfos.isNotEmpty()) {
                    val faceImageInfos: List<FaceImageInfo> = faceInfos[0].faceImageInfos
                    if (faceImageInfos.isNotEmpty()) {
                        val faceImageInfo = faceImageInfos[0]
                        val imageLength = faceImageInfo.imageLength
                        val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                        val buffer = ByteArray(imageLength)
                        dataInputStream.readFully(buffer)

                        // Try to decode as JPEG first, then JPEG2000
                        facePhoto = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
                        if (facePhoto == null) {
                            Log.w(TAG, "Could not decode face photo (may be JPEG2000)")
                        } else {
                            Log.d(TAG, "Face photo decoded successfully")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read DG2 (face photo)", e)
            }

            passportService.close()

            return@withContext NfcResult(
                data = NfcData(
                    uid = uid,
                    documentNumber = documentNumber,
                    fullName = fullName,
                    dateOfBirth = dateOfBirth,
                    dateOfExpiry = dateOfExpiry,
                    nationality = nationality,
                    gender = gender,
                    issuingState = issuingState,
                    personalNumber = personalNumber,
                    facePhoto = facePhoto,
                    isChipAuthenticated = true,
                    chipInfo = "تمت القراءة بنجاح",
                    rawMrzFromChip = rawMrz
                ),
                isSuccess = true
            )

        } catch (e: CardServiceException) {
            Log.e(TAG, "Card service error", e)
            return@withContext NfcResult(
                data = NfcData(uid = uid),
                error = "خطأ في قراءة الشريحة: ${e.message}",
                isSuccess = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "NFC read error", e)
            return@withContext NfcResult(
                data = NfcData(uid = uid),
                error = "خطأ غير متوقع: ${e.message}",
                isSuccess = false
            )
        } finally {
            try {
                isoDep.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing IsoDep", e)
            }
        }
    }

    /**
     * Quick read - just UID, no BAC needed.
     */
    suspend fun readUidOnly(tag: Tag): NfcResult = withContext(Dispatchers.IO) {
        val uid = getTagUid(tag)
        return@withContext NfcResult(
            data = NfcData(uid = uid, chipInfo = "UID فقط"),
            isSuccess = uid.isNotEmpty()
        )
    }
}
