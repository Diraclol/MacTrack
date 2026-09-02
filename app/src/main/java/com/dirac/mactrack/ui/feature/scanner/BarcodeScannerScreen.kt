package com.dirac.mactrack.ui.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.util.concurrent.atomic.AtomicBoolean

// Full-screen camera barcode scanner (UI-9). On-device / offline via ML Kit's bundled model. Uses the
// CameraX LifecycleCameraController + MlKitAnalyzer path, which owns the frame loop, rotation, and
// image lifecycle for us. On the first readable barcode it fires onResult(code) exactly once; the
// caller looks that code up in Open Food Facts (the "branded" source), reusing the manual path.
@Composable
fun BarcodeScannerScreen(
    onBack: () -> Unit,
    onResult: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }
    // Ask once on entry if we don't already hold the grant.
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val handled = remember { AtomicBoolean(false) }   // fire onResult exactly once
    val controller = remember { LifecycleCameraController(context) }
    var scannedCode by remember { mutableStateOf<String?>(null) }

    // Hand the hit off to the main thread, stop the camera, then report it.
    LaunchedEffect(scannedCode) {
        val code = scannedCode ?: return@LaunchedEffect
        controller.unbind()
        onResult(code)
    }

    DisposableEffect(Unit) {
        onDispose { controller.unbind() }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            hasCameraPermission -> {
                val previewView = remember {
                    PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                }
                AndroidView(
                    factory = {
                        previewView.controller = controller
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
                ScannerOverlay(modifier = Modifier.align(Alignment.Center))
                LaunchedEffect(controller) {
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E
                        )
                        .build()
                    val scanner = BarcodeScanning.getClient(options)
                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    controller.setImageAnalysisAnalyzer(
                        mainExecutor,
                        MlKitAnalyzer(
                            listOf(scanner),
                            ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                            mainExecutor
                        ) { result ->
                            val raw = result.getValue(scanner)
                                ?.firstOrNull { !it.rawValue.isNullOrBlank() }
                                ?.rawValue
                            if (raw != null && handled.compareAndSet(false, true)) {
                                controller.clearImageAnalysisAnalyzer()
                                scannedCode = raw
                            }
                        }
                    )
                    controller.bindToLifecycle(lifecycleOwner)
                }
                Text(
                    "Point the camera at a product barcode",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            permissionDenied -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Camera access is needed to scan barcodes.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp)
                    ) { Text("Grant camera access") }
                    TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Go back", color = Color.White)
                    }
                }
            }
        }

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Text("Back", color = Color.White)
        }
    }
}

// A viewfinder frame with a red line that sweeps up and down, so it reads as "scanning".
@Composable
private fun ScannerOverlay(modifier: Modifier = Modifier) {
    val frameW = 260.dp
    val frameH = 160.dp
    val transition = rememberInfiniteTransition(label = "scan")
    val pos by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = RepeatMode.Reverse),
        label = "line"
    )
    Box(
        modifier = modifier
            .width(frameW)
            .height(frameH)
            .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .offset(y = (frameH - 2.dp) * pos)
                .height(2.dp)
                .background(Color.Red)
        )
    }
}
