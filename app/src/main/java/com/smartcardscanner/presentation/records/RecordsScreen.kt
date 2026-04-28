package com.smartcardscanner.presentation.records

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcardscanner.domain.model.ScanRecord
import com.smartcardscanner.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    onRecordClick: (Long) -> Unit,
    viewModel: RecordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
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
                text = "السجلات",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            // Export button
            IconButton(
                onClick = {
                    viewModel.exportToExcel { file ->
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "تصدير السجلات"))
                    }
                }
            ) {
                Icon(Icons.Filled.FileDownload, "تصدير", tint = Color.White)
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في السجلات...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true
        )

        // Records count
        Text(
            text = "${uiState.records.size} سجل",
            fontSize = 13.sp,
            color = OnSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Records list
        if (uiState.records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Inbox,
                        null,
                        tint = OnSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "لا توجد سجلات محفوظة",
                        fontSize = 16.sp,
                        color = OnSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.records) { record ->
                    RecordCard(record = record, onClick = { onRecordClick(record.id) })
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: ScanRecord, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val matchColor = when {
        record.matchScore >= 0.95 -> SuccessGreen
        record.matchScore >= 0.85 -> PrimaryBlue
        record.matchScore >= 0.60 -> WarningOrange
        else -> ErrorRed
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Match indicator
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = matchColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${(record.matchScore * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = matchColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.nameFromDatabase.ifBlank {
                        record.nameFromNfc.ifBlank { record.nameFromBack.ifBlank { "بدون اسم" } }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface
                )
                Row {
                    Text(
                        text = dateFormat.format(Date(record.scanDate)),
                        fontSize = 12.sp,
                        color = OnSurface.copy(alpha = 0.5f)
                    )
                    if (record.ssn.isNotBlank()) {
                        Text(
                            text = " | SSN: ${record.ssn}",
                            fontSize = 12.sp,
                            color = OnSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    text = record.matchType.ifBlank { "—" },
                    fontSize = 12.sp,
                    color = matchColor
                )
            }

            // NFC status icon
            val nfcIcon = when {
                record.nfcStatus.contains("نجاح") || record.nfcStatus.contains("تم") ->
                    Icons.Filled.Nfc
                else -> Icons.Filled.NfcOff
            }
            val nfcColor = when {
                record.nfcStatus.contains("نجاح") || record.nfcStatus.contains("تم") ->
                    SuccessGreen
                else -> OnSurface.copy(alpha = 0.3f)
            }
            Icon(nfcIcon, null, tint = nfcColor, modifier = Modifier.size(20.dp))
        }
    }
}

// NfcOff icon fallback
private val Icons.Filled.NfcOff get() = Icons.Filled.MobileOff
