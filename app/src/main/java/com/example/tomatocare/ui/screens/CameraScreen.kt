package com.example.tomatocare.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraPreviewUseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Camera
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun CameraScreenPreview() {
    CameraScreen()
}

private val TomatoRed = Color(0xFFB3261D)
private val LeafGreen = Color(0xFF2C6A39)
private val LeafLight = Color(0xFFDBE6D5)
private val Background = Color(0xFFF7F4EB)
private val TextPrimary = Color(0xFF202320)
private val TextSecondary = Color(0xFF6B716B)
private val ViewfinderDark = Color(0xFF1E2A1E)

/**
 * Экран «Выбор фотографии».
 *
 * @param viewfinder  сюда позже подставляется превью CameraX (PreviewView через AndroidView).
 *                    Пока по умолчанию пусто — виден тёмный фон видоискателя.
 * @param onBack      нажатие на стрелку «назад».
 * @param onShutter   нажатие на кнопку съёмки.
 * @param onFlashChanged  переключение вспышки (true — включена).
 * @param onImagePicked   пользователь выбрал изображение в галерее.
 */
@Composable
fun CameraScreen(
    viewfinder: @Composable () -> Unit = {},
    onBack: () -> Unit = {},
    onPhotoCaptured: (Uri) -> Unit = {},
    onFlashChanged: (Boolean) -> Unit = {},
    onImagePicked: (Uri) -> Unit = {}
) {
    var flashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImagePicked(uri)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun takePhoto() {
        val capture = imageCapture ?: return

        val photoFile = File(
            context.cacheDir,
            "tomato_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            photoFile
        ).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    outputFileResults: ImageCapture.OutputFileResults
                ) {
                    val photoUri = Uri.fromFile(photoFile)
                    onPhotoCaptured(photoUri)
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    exception.printStackTrace()
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = "Выбор фотографии",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Viewfinder(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            content = {
                if (hasCameraPermission) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onCameraReady = { cameraInstance, capture ->
                            camera = cameraInstance
                            imageCapture = capture
                        }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SideButton(
                icon = Icons.Default.PhotoLibrary,
                description = "Галерея",
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            ShutterButton(
                onClick = {
                    takePhoto()
                }
            )

            SideButton(
                icon = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                description = if (flashOn) "Выключить вспышку" else "Включить вспышку",
                onClick = {
                    val newFlashState = !flashOn

                    camera?.cameraControl?.enableTorch(newFlashState)

                    flashOn = newFlashState
                    onFlashChanged(newFlashState)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Хорошее освещение, лист целиком,\nбез сильных бликов",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Viewfinder(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(ViewfinderDark),
        contentAlignment = Alignment.Center
    ) {
        // Сюда встанет превью камеры.
        content()

        // Пунктирная рамка для листа.
        Canvas(modifier = Modifier.size(240.dp)) {
            val strokeWidth = 3.dp.toPx()
            val half = strokeWidth / 2
            drawRoundRect(
                color = LeafLight,
                topLeft = Offset(half, half),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(14.dp.toPx(), 10.dp.toPx())
                    )
                )
            )
        }

        Text(
            text = "Расположите один лист в рамке",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            fontSize = 14.sp,
            color = LeafLight
        )
    }
}

@Composable
private fun SideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, LeafGreen),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = LeafGreen
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit
) {
    // Внешнее красное кольцо + внутренний сплошной красный круг.
    Box(
        modifier = Modifier
            .size(84.dp)
            .border(3.dp, TomatoRed, CircleShape)
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .background(TomatoRed, CircleShape)
                .semantics { contentDescription = "Сделать снимок" }
        ) {}
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (Camera, ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        update = { previewView ->
            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = CameraPreviewUseCase.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onCameraReady(camera, imageCapture)
            }, ContextCompat.getMainExecutor(context))
        }
    )
}