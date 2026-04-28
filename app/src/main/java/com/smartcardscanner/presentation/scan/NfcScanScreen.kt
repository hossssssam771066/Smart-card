package com.smartcardscanner.presentation.scan

import android.nfc.NfcAdapter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcardscanner.MainActivity
import com.smartcardscanner.presentation.theme.*

@Composable
fun NfcScanScreen(
    onScanComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Check NFC hardware availability
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val hasNfcHardware = nfcAdapter != null
    val isNfcEnabled = nfcAdapter?.isEnabled == true

    // NFC chip detection state
    var chipDetected by remember { mutableStateOf(false) }
    var chipUid by remember { mutableStateOf("") }
    var detectionDone by remember { mutableStateOf(false) }

    // Register NFC callback with Activity for chip detection
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.setNfcCallback { tag ->
            chipDetected = true
            chipUid = tag.id?.joinToString("") { "%02X".format(it) } ?: ""
            detectionDone = true
            // Store NFC detection info in viewModel
            viewModel.onNfcChipDetected(chipDetected, chipUid, tag.techList?.joinToString(", ") ?: "")
        }
        onDispose {
            activity?.setNfcCallback(null)
        }
    }

    // Auto-proceed to results after detection
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == ScanPhase.NFC_COMPLETE || uiState.phase == ScanPhase.RESULTS_READY) {
            onScanComplete()
        }
    }

    // NFC pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Background, Color.White)
                )
            )
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
                text = "فحص شريحة NFC",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // MRZ data summary from previous step
        if (uiState.isBackScanSuccess && uiState.mrzData != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("تم مسح الباركود", fontSize = 14.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        Text(
                            "الاسم: ${uiState.mrzData?.fullNameEnglish ?: ""}",
                            fontSize = 12.sp,
                            color = OnSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (!hasNfcHardware) {
                // Device doesn't have NFC
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.PhonelinkErase,
                        null,
                        tint = ErrorRed,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "هذا الجهاز لا يدعم NFC",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لا يمكن فحص وجود الشريحة",
                        fontSize = 14.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (!isNfcEnabled) {
                // NFC is off
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Filled.SignalWifiOff,
                        null,
                        tint = WarningOrange,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "NFC معطّل",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "فعّل NFC من الإعدادات ثم أعد المحاولة",
                        fontSize = 14.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (detectionDone) {
                // Detection result
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    if (chipDetected) {
                        // Chip found!
                        Icon(
                            Icons.Filled.VerifiedUser,
                            null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "تم اكتشاف شريحة NFC",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "البطاقة تحتوي على شريحة إلكترونية",
                            fontSize = 14.sp,
                            color = OnSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        if (chipUid.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "UID: $chipUid",
                                fontSize = 12.sp,
                                color = OnSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint = ErrorRed,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "لا توجد شريحة NFC",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Waiting for card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // Pulse circles
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .background(PrimaryBlue.copy(alpha = 0.2f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(pulseScale * 0.9f)
                                .alpha(pulseAlpha * 1.5f)
                                .background(PrimaryBlue.copy(alpha = 0.3f), CircleShape)
                        )
                        Icon(
                            Icons.Filled.Nfc,
                            null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "قرّب البطاقة من الهاتف",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لفحص وجود شريحة NFC",
                        fontSize = 14.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (detectionDone && chipDetected) {
                Button(
                    onClick = {
                        viewModel.skipNfc()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("متابعة — مطابقة مع القاعدة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = {
                    if (!detectionDone) {
                        detectionDone = true
                        chipDetected = false
                        viewModel.onNfcChipDetected(false, "", "")
                    }
                    viewModel.skipNfc()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (detectionDone) "تخطي — الذهاب للنتائج" else "لا توجد شريحة — تخطي",
                    fontSize = 14.sp
                )
            }
        }
    }
}
