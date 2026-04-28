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
            // === PRIMARY RESULT: Match with Database ===
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
            val bannerIcon = when (match?.matchType) {
                MatchType.EXACT, MatchType.STRONG -> Icons.Filled.CheckCircle
                MatchType.POSSIBLE -> Icons.Filled.Warning
                else -> Icons.Filled.Cancel
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
                    Icon(bannerIcon, null, tint = bannerColor, modifier = Modifier.size(32.dp))
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

            // === MAIN RESULT CARD: Name from DB + SSN ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "نتيجة المطابقة",
                        fontSize = 14.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (match != null) {
                        // Name from database
                        Text(
                            "الاسم في القاعدة",
                            fontSize = 13.sp,
                            color = OnSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            match.personnel.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = PrimaryBlue.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // SSN / Code
                        Text(
                            "الكود (SSN)",
                            fontSize = 13.sp,
                            color = OnSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            match.personnel.ssn,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            textAlign = TextAlign.Center
                        )

                        // Additional DB fields
                        if (match.personnel.militaryNumber.isNotBlank() ||
                            match.personnel.rank.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = PrimaryBlue.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            if (match.personnel.rank.isNotBlank()) {
                                ResultFieldRow("الرتبة", match.personnel.rank)
                            }
                            if (match.personnel.militaryNumber.isNotBlank()) {
                                ResultFieldRow("الرقم العسكري", match.personnel.militaryNumber)
                            }
                            if (match.personnel.mainUnit.isNotBlank()) {
                                ResultFieldRow("الوحدة الرئيسية", match.personnel.mainUnit)
                            }
                            if (match.personnel.subUnit.isNotBlank()) {
                                ResultFieldRow("الوحدة الفرعية", match.personnel.subUnit)
                            }
                            if (match.personnel.militaryCard.isNotBlank()) {
                                ResultFieldRow("البطاقة العسكرية", match.personnel.militaryCard)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Icon(Icons.Filled.SearchOff, null, tint = ErrorRed.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لم يتم العثور على تطابق في القاعدة",
                            fontSize = 16.sp,
                            color = OnSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // === NFC Chip Status ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isNfcSuccess) SuccessGreen.copy(alpha = 0.08f)
                    else WarningOrange.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (uiState.isNfcSuccess) Icons.Filled.Nfc else Icons.Filled.SignalWifiOff,
                        null,
                        tint = if (uiState.isNfcSuccess) SuccessGreen else WarningOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "حالة الشريحة",
                            fontSize = 12.sp,
                            color = OnSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            uiState.nfcStatus,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isNfcSuccess) SuccessGreen else WarningOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // === Extracted Data from Barcode ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "البيانات المستخرجة من الباركود",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultFieldRow("الاسم", uiState.nameFromBack)
                    uiState.mrzData?.let { mrz ->
                        ResultFieldRow("رقم الوثيقة", mrz.documentNumber)
                        ResultFieldRow("الجنسية", com.smartcardscanner.device.barcode.MrzParser().formatNationality(mrz.nationality))
                        ResultFieldRow("تاريخ الميلاد", com.smartcardscanner.device.barcode.MrzParser().formatDateOfBirth(mrz.dateOfBirth))
                        ResultFieldRow("الجنس", com.smartcardscanner.device.barcode.MrzParser().formatGender(mrz.sex))
                    }
                    ResultFieldRow("حالة المسح", uiState.backScanStatus)
                }
            }

            // Multiple match suggestions
            if (uiState.matchResults.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "اقتراحات أخرى",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.matchResults.forEachIndexed { index, result ->
                            if (result != uiState.selectedMatch) {
                                MatchSuggestionRow(
                                    result = result,
                                    onClick = { viewModel.selectMatch(result) }
                                )
                                if (index < uiState.matchResults.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
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
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isSaved) "تم الحفظ" else "حفظ")
                }
                OutlinedButton(
                    onClick = onNewScan,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مسح جديد")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ResultFieldRow(label: String, value: String) {
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
            color = OnSurface.copy(alpha = 0.5f),
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.65f)
        )
    }
}

@Composable
private fun MatchSuggestionRow(
    result: MatchResult,
    onClick: () -> Unit
) {
    val scoreColor = when {
        result.score >= 0.95 -> SuccessGreen
        result.score >= 0.85 -> PrimaryBlue
        result.score >= 0.60 -> WarningOrange
        else -> ErrorRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                result.personnel.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "SSN: ${result.personnel.ssn}",
                fontSize = 12.sp,
                color = OnSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            "${(result.score * 100).toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = scoreColor
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
