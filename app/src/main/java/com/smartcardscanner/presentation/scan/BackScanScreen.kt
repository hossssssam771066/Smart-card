package com.smartcardscanner.presentation.scan

import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartcardscanner.device.barcode.CardBackScanner
import com.smartcardscanner.device.barcode.MrzParser
import com.smartcardscanner.presentation.theme.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BackScanScreen(
    onScanComplete: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("وجّه الكاميرا نحو خلفية البطاقة") }
    val scanner = remember { CardBackScanner(context, MrzParser()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val mrzParserInstance = remember { MrzParser() }

    // Show extracted data when scan is complete
    var showExtractedData by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isBackScanSuccess) {
        if (uiState.isBackScanSuccess) {
            showExtractedData = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (showExtractedData) Background else Color.Black)
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
                text = if (showExtractedData) "البيانات المستخرجة" else "مسح خلفية البطاقة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            if (!showExtractedData) {
                IconButton(onClick = {
                    isFlashOn = !isFlashOn
                    camera?.cameraControl?.enableTorch(isFlashOn)
                }) {
                    Icon(
                        if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        "فلاش",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (showExtractedData) {
            // Show extracted fields
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Success banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "تم مسح الباركود بنجاح",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Extracted MRZ data fields
                uiState.mrzData?.let { mrz ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "بيانات MRZ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ExtractedField("الاسم (إنجليزي)", mrz.fullNameEnglish)
                            ExtractedField("رقم الوثيقة", mrz.documentNumber)
                            ExtractedField("الجنسية", mrzParserInstance.formatNationality(mrz.nationality))
                            ExtractedField("تاريخ الميلاد", mrzParserInstance.formatDateOfBirth(mrz.dateOfBirth))
                            ExtractedField("الجنس", mrzParserInstance.formatGender(mrz.sex))
                            ExtractedField("تاريخ الانتهاء", mrzParserInstance.formatDateOfBirth(mrz.dateOfExpiry))
                            ExtractedField("الدولة المصدرة", mrzParserInstance.formatNationality(mrz.issuingCountry))
                        }
                    }
                }

                // Barcode data if available
                uiState.barcodeData?.let { barcode ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "بيانات الباركود",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ExtractedField("النوع", barcode.format)
                            ExtractedField("القيمة", barcode.rawValue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Button(
                    onClick = { onScanComplete() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Filled.Nfc, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("التالي — فحص شريحة NFC", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.skipBackScan()
                        onSkip()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تخطي فحص NFC", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        showExtractedData = false
                        isScanning = true
                        viewModel.resetScan()
                        statusMessage = "وجّه الكاميرا نحو خلفية البطاقة"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعادة المسح", fontSize = 14.sp)
                }
            }
        } else {
            // Camera preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            @Suppress("DEPRECATION")
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                if (!isScanning) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                scope.launch {
                                    try {
                                        val result = scanner.processImage(imageProxy)
                                        if (result.isSuccess) {
                                            isScanning = false
                                            statusMessage = "تم المسح بنجاح!"
                                            viewModel.onBackScanResult(result)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("BackScan", "Error processing image", e)
                                    }
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("BackScan", "Camera bind failed", e)
                            }

                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Scan guide overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.586f)
                            .border(
                                width = 2.dp,
                                color = if (uiState.isBackScanSuccess) SuccessGreen else Color.White.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            // Status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryDark.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtractedField(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = OnSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f)
        )
    }
}
