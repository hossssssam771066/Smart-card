package com.smartcardscanner.presentation.scan

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

    // Register NFC callback with Activity
    DisposableEffect(Unit) {
        val activity = context as? MainActivity
        activity?.setNfcCallback { tag ->
            viewModel.onNfcTagDiscovered(tag)
        }
        onDispose {
            activity?.setNfcCallback(null)
        }
    }

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
                text = "قراءة NFC",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // MRZ status from previous step
        if (uiState.isBackScanSuccess) {
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
                        Text("تم مسح الخلفية بنجاح", fontSize = 14.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        if (uiState.mrzData != null) {
                            Text(
                                "رقم الوثيقة: ${uiState.mrzData?.documentNumber}",
                                fontSize = 12.sp,
                                color = OnSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // NFC animation area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    color = PrimaryBlue
                )
                Text(
                    text = "جاري قراءة الشريحة...",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    fontSize = 16.sp,
                    color = PrimaryBlue
                )
            } else if (uiState.isNfcSuccess) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "تم قراءة الشريحة بنجاح!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        // Pulse ring
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .scale(pulseScale)
                                .alpha(pulseAlpha)
                                .background(
                                    color = PrimaryLight.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                        // NFC icon
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = PrimaryBlue
                        ) {
                            Icon(
                                Icons.Filled.Nfc,
                                null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "قرّب البطاقة من خلف الهاتف",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "ابقِ البطاقة ثابتة حتى تكتمل القراءة",
                        fontSize = 14.sp,
                        color = OnSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Error display
        if (uiState.error != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.error ?: "", fontSize = 14.sp, color = ErrorRed)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.skipNfc()
                    onSkip()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تخطي NFC — الاستمرار بدون شريحة", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
