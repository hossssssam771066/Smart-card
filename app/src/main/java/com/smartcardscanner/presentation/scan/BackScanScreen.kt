package com.smartcardscanner.presentation.scan

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.smartcardscanner.device.barcode.CardBackScanner
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
    val scanner = remember { CardBackScanner(context, com.smartcardscanner.device.barcode.MrzParser()) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(uiState.isBackScanSuccess) {
        if (uiState.isBackScanSuccess) {
            onScanComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
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
                text = "مسح خلفية البطاقة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
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
        }

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
                                    Log.e("BackScan", "Error", e)
                                }
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
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

            // Scan overlay frame
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(190.dp)
                        .border(
                            width = 3.dp,
                            color = if (uiState.isBackScanSuccess) SuccessGreen else PrimaryLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }
        }

        // Status and controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryDark.copy(alpha = 0.95f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = statusMessage,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            if (uiState.mrzData != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MRZ: ${uiState.mrzData?.documentNumber ?: ""}",
                    color = SuccessGreen,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.skipBackScan()
                        onSkip()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تخطي")
                }

                Button(
                    onClick = {
                        isScanning = true
                        statusMessage = "وجّه الكاميرا نحو خلفية البطاقة"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة المسح")
                }
            }
        }
    }
}
