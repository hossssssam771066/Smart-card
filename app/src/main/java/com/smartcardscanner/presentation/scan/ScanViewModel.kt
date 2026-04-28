package com.smartcardscanner.presentation.scan

import android.content.Context
import android.graphics.Bitmap
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcardscanner.data.repository.PersonnelRepository
import com.smartcardscanner.data.repository.ScanRepository
import com.smartcardscanner.device.barcode.CardBackScanner
import com.smartcardscanner.device.barcode.MrzParser
import com.smartcardscanner.device.nfc.NfcReader
import com.smartcardscanner.domain.matching.ArabicNameMatcher
import com.smartcardscanner.domain.matching.ArabicNameNormalizer
import com.smartcardscanner.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ScanUiState(
    val phase: ScanPhase = ScanPhase.IDLE,
    // Back scan data
    val mrzData: MrzData? = null,
    val barcodeData: BarcodeData? = null,
    val backScanText: String = "",
    val nameFromBack: String = "",
    val nameArabicFromBack: String = "",
    val nationalIdFromBack: String = "",
    val dateOfBirthFromBack: String = "",
    val placeOfBirthFromBack: String = "",
    val governorateFromBack: String = "",
    val districtFromBack: String = "",
    val motherNameFromBack: String = "",
    val backScanStatus: String = "",
    val isBackScanSuccess: Boolean = false,
    // NFC data
    val nfcData: NfcData? = null,
    val nfcStatus: String = "",
    val isNfcSuccess: Boolean = false,
    val facePhoto: Bitmap? = null,
    // Match data
    val matchResults: List<MatchResult> = emptyList(),
    val selectedMatch: MatchResult? = null,
    // Combined result
    val scanRecord: ScanRecord? = null,
    val isSaved: Boolean = false,
    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

enum class ScanPhase {
    IDLE,
    SCANNING_BACK,
    BACK_COMPLETE,
    WAITING_NFC,
    READING_NFC,
    NFC_COMPLETE,
    MATCHING,
    RESULTS_READY,
    SAVED
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val nfcReader: NfcReader,
    private val mrzParser: MrzParser,
    private val nameMatcher: ArabicNameMatcher,
    private val nameNormalizer: ArabicNameNormalizer,
    private val personnelRepository: PersonnelRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ScanViewModel"
    }

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun resetScan() {
        _uiState.value = ScanUiState()
    }

    // --- Back Scan ---

    fun onBackScanResult(result: CardBackScanner.ScanResult) {
        val nameEnglish = result.nameEnglish.ifBlank { result.mrzData?.fullNameEnglish.orEmpty() }
        val status = if (result.isSuccess) "تم بنجاح" else "فشل"

        _uiState.value = _uiState.value.copy(
            mrzData = result.mrzData,
            barcodeData = result.barcodeData,
            backScanText = result.ocrText,
            nameFromBack = nameEnglish,
            nameArabicFromBack = result.nameArabic,
            nationalIdFromBack = result.nationalId,
            dateOfBirthFromBack = result.dateOfBirth,
            placeOfBirthFromBack = result.placeOfBirth,
            governorateFromBack = result.governorate,
            districtFromBack = result.district,
            motherNameFromBack = result.motherName,
            backScanStatus = status,
            isBackScanSuccess = result.isSuccess,
            phase = ScanPhase.BACK_COMPLETE
        )

        Log.d(
            TAG,
            "Back scan complete: MRZ=${result.mrzData != null}, ar='${result.nameArabic}', en='$nameEnglish'"
        )

        // The user wants matching to happen the moment the barcode is scanned
        // — don't wait for NFC. Kick off matching now.
        viewModelScope.launch { performMatching() }
    }

    fun skipBackScan() {
        _uiState.value = _uiState.value.copy(
            backScanStatus = "تم التخطي",
            phase = ScanPhase.BACK_COMPLETE
        )
    }

    // --- NFC ---

    fun onNfcTagDiscovered(tag: Tag) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                phase = ScanPhase.READING_NFC,
                isLoading = true
            )

            val bacKey = _uiState.value.mrzData?.bacKey
            val result = nfcReader.readCard(tag, bacKey)

            if (result.isSuccess && result.data != null) {
                _uiState.value = _uiState.value.copy(
                    nfcData = result.data,
                    nfcStatus = "تم بنجاح",
                    isNfcSuccess = true,
                    facePhoto = result.data.facePhoto,
                    phase = ScanPhase.NFC_COMPLETE,
                    isLoading = false
                )
                // Auto-proceed to matching
                performMatching()
            } else {
                _uiState.value = _uiState.value.copy(
                    nfcData = result.data,
                    nfcStatus = result.error ?: "فشل القراءة",
                    isNfcSuccess = false,
                    phase = ScanPhase.NFC_COMPLETE,
                    isLoading = false,
                    error = result.error
                )
            }
        }
    }

    fun onNfcChipDetected(detected: Boolean, uid: String, techList: String) {
        val status = if (detected) "شريحة موجودة (UID: $uid)" else "لا توجد شريحة"
        val nfcData = if (detected) NfcData(
            uid = uid,
            chipInfo = "شريحة NFC مكتشفة — $techList"
        ) else null

        _uiState.value = _uiState.value.copy(
            nfcData = nfcData,
            nfcStatus = status,
            isNfcSuccess = detected
        )
    }

    fun skipNfc() {
        _uiState.value = _uiState.value.copy(
            phase = ScanPhase.NFC_COMPLETE
        )
        if (_uiState.value.nfcStatus.isBlank()) {
            _uiState.value = _uiState.value.copy(nfcStatus = "تم التخطي")
        }
        viewModelScope.launch {
            performMatching()
        }
    }

    // --- Matching ---

    private suspend fun performMatching() {
        _uiState.value = _uiState.value.copy(
            phase = ScanPhase.MATCHING,
            isLoading = true
        )

        val state = _uiState.value
        val results = mutableListOf<MatchResult>()

        // 1) Try the national id from the barcode payload (xxxx-xxxx-xxxx).
        //    On Yemeni eID this is the same number stored in the SSN column
        //    of the personnel database, so an exact match here is gold.
        val nationalIdCandidates = listOfNotNull(
            state.nationalIdFromBack.takeIf { it.isNotBlank() },
            state.mrzData?.documentNumber?.takeIf { it.isNotBlank() },
            state.nfcData?.documentNumber?.takeIf { it.isNotBlank() }
        )
        for (nid in nationalIdCandidates) {
            val candidates = personnelRepository.searchByName("")
            val idMatch = nameMatcher.matchByNationalId(nid, candidates)
            if (idMatch != null) {
                results.add(idMatch)
                break
            }
        }

        // 2) Smart-match by name. Prefer the Arabic name from the barcode
        //    (because the personnel database column "الاسم" is in Arabic).
        //    Fall back to NFC arabic name, then OCR/MRZ english name.
        val nameCandidates = listOfNotNull(
            state.nameArabicFromBack.takeIf { it.isNotBlank() },
            state.nfcData?.fullName?.takeIf { it.isNotBlank() },
            state.nameFromBack.takeIf { it.isNotBlank() },
            state.mrzData?.fullNameEnglish?.takeIf { it.isNotBlank() }
        )
        if (results.isEmpty()) {
            for (name in nameCandidates) {
                val candidates = personnelRepository.searchByName(name)
                val nameResults = nameMatcher.matchByName(name, candidates)
                if (nameResults.isNotEmpty()) {
                    results.addAll(nameResults)
                    break
                }
            }
        }

        val selectedMatch = results.firstOrNull { it.matchType == MatchType.EXACT }
            ?: results.firstOrNull { it.matchType == MatchType.STRONG }
            ?: results.firstOrNull()

        _uiState.value = _uiState.value.copy(
            matchResults = results,
            selectedMatch = selectedMatch,
            phase = ScanPhase.RESULTS_READY,
            isLoading = false
        )
    }

    fun selectMatch(matchResult: MatchResult) {
        _uiState.value = _uiState.value.copy(selectedMatch = matchResult)
    }

    // --- Save ---

    fun saveRecord(context: Context, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val match = state.selectedMatch
            val mrzParser = MrzParser()

            // Save face photo if available
            var photoPath = ""
            state.facePhoto?.let { bitmap ->
                try {
                    val dir = File(context.filesDir, "photos")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "face_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    photoPath = file.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save face photo", e)
                }
            }

            val matchTypeStr = when (match?.matchType) {
                MatchType.EXACT -> "تطابق مباشر"
                MatchType.STRONG -> "تطابق قوي"
                MatchType.POSSIBLE -> "تطابق محتمل"
                MatchType.NONE -> "غير موجود"
                null -> "لم يتم المطابقة"
            }

            val record = ScanRecord(
                scanDate = System.currentTimeMillis(),
                nameFromBack = state.nameFromBack,
                nameFromNfc = state.nfcData?.fullName ?: "",
                nameFromDatabase = match?.personnel?.name ?: "",
                ssn = match?.personnel?.ssn ?: "",
                nationalId = state.mrzData?.documentNumber ?: state.nfcData?.documentNumber ?: "",
                militaryNumber = match?.personnel?.militaryNumber ?: "",
                rank = match?.personnel?.rank ?: "",
                mainUnit = match?.personnel?.mainUnit ?: "",
                subUnit = match?.personnel?.subUnit ?: "",
                militaryCard = match?.personnel?.militaryCard ?: "",
                matchScore = match?.score ?: 0.0,
                matchType = matchTypeStr,
                nfcStatus = state.nfcStatus,
                backScanStatus = state.backScanStatus,
                nfcUid = state.nfcData?.uid ?: "",
                documentNumber = state.mrzData?.documentNumber ?: state.nfcData?.documentNumber ?: "",
                dateOfBirth = state.mrzData?.dateOfBirth?.let { mrzParser.formatDateOfBirth(it) }
                    ?: state.nfcData?.dateOfBirth ?: "",
                gender = state.mrzData?.sex?.let { mrzParser.formatGender(it) }
                    ?: state.nfcData?.gender ?: "",
                nationality = state.mrzData?.nationality?.let { mrzParser.formatNationality(it) }
                    ?: state.nfcData?.nationality ?: "",
                notes = notes,
                facePhotoPath = photoPath
            )

            val id = scanRepository.insert(record)

            _uiState.value = _uiState.value.copy(
                scanRecord = record.copy(id = id),
                isSaved = true,
                phase = ScanPhase.SAVED,
                message = "تم الحفظ بنجاح"
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
