package com.calculadoracalorias.app.presentation.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.calculadoracalorias.app.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCameraScreen(
    onImageCaptured: (ByteArray) -> Unit,
    isCloudAiActive: Boolean = false,
    onAnalyzeTextDescription: ((String) -> Unit)? = null,
    isAnalyzingText: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToManualEntry: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    BackHandler { onNavigateBack() }

    var showTextEntrySheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isCapturing = false
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isCapturing = true
            val bytes = context.contentResolver.openInputStream(it)?.use { stream ->
                stream.readBytes()
            }
            if (bytes != null && bytes.isNotEmpty()) {
                val compressedBytes = compressAndResizeImage(bytes)
                onImageCaptured(compressedBytes)
            } else {
                isCapturing = false
            }
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionFallback(onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        })
        return
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vista de Cámara
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener(
                    Runnable {
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = CameraPreview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            cameraInstance = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (_: Exception) {
                            // Error de inicialización de cámara
                        }
                    },
                    ContextCompat.getMainExecutor(ctx)
                )
                previewView
            }
        )

        // Encabezado superior en 2 niveles (Navegación + Guía contextual)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.60f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Nivel 1: Fila de Navegación y Accesos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Volver + Título + Insignia IA
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "🥑 Palta",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        color = if (isCloudAiActive) Color(0xFF1D4724).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCloudAiActive) Color(0xFF8CE593).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = if (isCloudAiActive) stringResource(id = R.string.badge_engine_cloud) else stringResource(id = R.string.badge_engine_local),
                            color = if (isCloudAiActive) Color(0xFFA8F7AF) else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Acción a la derecha (Registro manual)
                IconButton(
                    onClick = onNavigateToManualEntry,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Registro manual",
                        tint = Color.White
                    )
                }
            }

            // Nivel 2: Subtítulo contextual centrado y no invasivo
            Surface(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally),
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.camera_instruction),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Único botón de interacción para describir con IA (Píldora inferior destacada)
        Button(
            onClick = { showTextEntrySheet = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 110.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.btn_describe_with_ai),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        // Barra inferior de controles de captura ergonómicos
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Galería
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(id = R.string.btn_select_gallery),
                    tint = Color.White
                )
            }

            // Botón Capturar Foto (Obturador principal)
            IconButton(
                onClick = {
                    if (isCapturing) return@IconButton
                    isCapturing = true
                    imageCapture.takePicture(
                        cameraExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                val buffer = imageProxy.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                imageProxy.close()

                                val compressedBytes = compressAndResizeImage(bytes)
                                ContextCompat.getMainExecutor(context).execute {
                                    onImageCaptured(compressedBytes)
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                imageProxyError(exception)
                                ContextCompat.getMainExecutor(context).execute {
                                    isCapturing = false
                                }
                            }
                        }
                    )
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        if (isCapturing) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            ) {
                if (isCapturing) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(id = R.string.btn_take_photo),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Linterna / Flash
            IconButton(
                onClick = {
                    cameraInstance?.let { cam ->
                        val nextFlashState = !isFlashOn
                        if (cam.cameraInfo.hasFlashUnit()) {
                            cam.cameraControl.enableTorch(nextFlashState)
                            isFlashOn = nextFlashState
                        }
                    }
                },
                enabled = !isCapturing,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = stringResource(id = R.string.btn_toggle_flash),
                    tint = if (isFlashOn) Color.Yellow else Color.White
                )
            }
        }

        if (showTextEntrySheet) {
            QuickNaturalLanguageEntrySheet(
                isAnalyzing = isAnalyzingText,
                onDismiss = { showTextEntrySheet = false },
                onAnalyzeDescription = { description ->
                    onAnalyzeTextDescription?.invoke(description)
                }
            )
        }
    }
}

/**
 * Optimiza y redimensiona la imagen capturada o importada para análisis con IA de visión.
 * Reduce la dimensión máxima a 1024px respetando la orientación EXIF y comprime en JPEG de alta eficiencia.
 */
private fun compressAndResizeImage(
    imageBytes: ByteArray,
    maxDimension: Int = 1024,
    quality: Int = 85
): ByteArray {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight
        if (origWidth <= 0 || origHeight <= 0) return imageBytes

        var inSampleSize = 1
        var w = origWidth
        var h = origHeight
        while (w > maxDimension * 2 || h > maxDimension * 2) {
            w /= 2
            h /= 2
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, decodeOptions) ?: return imageBytes

        // Orientación EXIF
        val rotationDegrees = try {
            val exif = ExifInterface(ByteArrayInputStream(imageBytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }

        val orientedBitmap = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } else {
            bitmap
        }

        val curW = orientedBitmap.width
        val curH = orientedBitmap.height
        val scaledBitmap = if (curW > maxDimension || curH > maxDimension) {
            val ratio = curW.toFloat() / curH.toFloat()
            val targetW: Int
            val targetH: Int
            if (ratio > 1f) {
                targetW = maxDimension
                targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetH = maxDimension
                targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            val scaled = Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
            if (scaled != orientedBitmap) orientedBitmap.recycle()
            scaled
        } else {
            orientedBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        scaledBitmap.recycle()
        outputStream.toByteArray()
    } catch (e: Exception) {
        imageBytes
    }
}

private fun imageProxyError(exception: ImageCaptureException) {
    // Log exception
}

@Composable
private fun CameraPermissionFallback(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.camera_permission_required),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(id = R.string.btn_grant_permission))
        }
    }
}

// -------------------------------------------------------------
// Compose Previews (Requisito 4 & Tarea 3.5)
// -------------------------------------------------------------

@androidx.compose.ui.tooling.preview.Preview(name = "Permiso de Cámara Requerido", showBackground = true)
@Composable
private fun PreviewCameraPermissionFallback() {
    com.calculadoracalorias.app.presentation.theme.CalculadoraCaloriasTheme {
        CameraPermissionFallback(onRequestPermission = {})
    }
}
