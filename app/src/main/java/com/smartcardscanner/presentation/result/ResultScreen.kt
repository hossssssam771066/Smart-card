package com.smartcardscanner.presentation.result

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcardscanner.domain.model.MatchResult
import com.smartcardscanner.domain.model.MatchType
import com.smartcardscanner.presentation.scan.ScanViewModel
import com.smartcardscanner.presentation.theme.*

@Composable
fun ResultScreen(
    onSave: () -> Unit,
    onNewScan: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            // Stay on screen to show saved state
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryDark)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowForward, "رجوع", tint = Color.White)
            }
            Text(
                text = "النتائج",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Match result banner
            val match = uiState.selectedMatch
            val bannerColor = when (match?.matchType) {
                MatchType.EXACT -> SuccessGreen
                MatchType.STRONG -> PrimaryBlue
                MatchType.POSSIBLE -> WarningOrange
                MatchType.NONE, null -> ErrorRed
            }
            val bannerText = when (match?.matchType) {
                MatchType.EXACT -> "تطابق مباشر — ${(match.score * 100).toInt()}%"
                MatchType.STRONG -> "تطابق قوي — ${(match.score * 100).toInt()}%"
                MatchType.POSSIBLE -> "تطابق محتمل — ${(match.score * 100).toInt()}%"
                MatchType.NONE -> "غير موجود في القاعدة"
                null -> "لم يتم المطابقة"
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (match?.matchType) {
                        MatchType.EXACT, MatchType.STRONG -> Icons.Filled.CheckCircle
                        MatchType.POSSIBLE -> Icons.Filled.Warning
                        else -> Icons.Filled.Cancel
                    }
                    Icon(icon, null, tint = bannerColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = bannerText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Face photo from NFC
            uiState.facePhoto?.let { photo ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الصورة من NFC", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "صورة الوجه",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(3.dp, PrimaryBlue, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Data sections
            SectionCard("بيانات الخلفية") {
                DataRow("الاسم من الخلفية", uiState.nameFromBack)
                DataRow("حالة المسح", uiState.backScanStatus)
                uiState.mrzData?.let { mrz ->
                    DataRow("رقم الوثيقة", mrz.documentNumber)
                    DataRow("الاسم (MRZ)", mrz.fullNameEnglish)
                    DataRow("الجنسية", com.smartcardscanner.device.barcode.MrzParser().formatNationality(mrz.nationality))
                    DataRow("تاريخ الميلاد", com.smartcardscanner.device.barcode.MrzParser().formatDateOfBirth(mrz.dateOfBirth))
                    DataRow("الجنس", com.smartcardscanner.device.barcode.MrzParser().formatGender(mrz.sex))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("بيانات NFC") {
                DataRow("حالة NFC", uiState.nfcStatus)
                uiState.nfcData?.let { nfc ->
                    DataRow("UID", nfc.uid)
                    if (nfc.fullName.isNotBlank()) DataRow("الاسم من NFC", nfc.fullName)
                    if (nfc.documentNumber.isNotBlank()) DataRow("رقم الوثيقة", nfc.documentNumber)
                    if (nfc.dateOfBirth.isNotBlank()) DataRow("تاريخ الميلاد", nfc.dateOfBirth)
                    if (nfc.nationality.isNotBlank()) DataRow("الجنسية", nfc.nationality)
                    if (nfc.gender.isNotBlank()) DataRow("الجنس", nfc.gender)
                    DataRow("حالة الشريحة", nfc.chipInfo)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SectionCard("بيانات القاعدة") {
                if (match != null) {
                    DataRow("الاسم حسب القاعدة", match.personnel.name)
                    DataRow("SSN", match.personnel.ssn)
                    DataRow("الرقم العسكري", match.personnel.militaryNumber)
                    DataRow("الرتبة", match.personnel.rank)
                    DataRow("الوحدة الرئيسية", match.personnel.mainUnit)
                    DataRow("الوحدة الفرعية", match.personnel.subUnit)
                    DataRow("البطاقة العسكرية", match.personnel.militaryCard)
                } else {
                    Text(
                        "لم يتم العثور على تطابق في القاعدة",
                        fontSize = 14.sp,
                        color = OnSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Multiple match suggestions
            if (uiState.matchResults.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionCard("اقتراحات أخرى") {
                    uiState.matchResults.forEachIndexed { index, result ->
                        if (result != uiState.selectedMatch) {
                            MatchSuggestionRow(
                                result = result,
                                onClick = { viewModel.selectMatch(result) }
                            )
                            if (index < uiState.matchResults.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.saveRecord(context, notes) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    enabled = !uiState.isSaved
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isSaved) "تم الحفظ" else "حفظ")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetScan()
                        onNewScan()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسح جديد")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun MatchSuggestionRow(result: MatchResult, onClick: () -> Unit) {
    val scoreColor = when (result.matchType) {
        MatchType.EXACT -> SuccessGreen
        MatchType.STRONG -> PrimaryBlue
        MatchType.POSSIBLE -> WarningOrange
        MatchType.NONE -> ErrorRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(result.personnel.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("SSN: ${result.personnel.ssn}", fontSize = 12.sp, color = OnSurface.copy(alpha = 0.5f))
        }
        Text(
            "${(result.score * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )
    }
}
