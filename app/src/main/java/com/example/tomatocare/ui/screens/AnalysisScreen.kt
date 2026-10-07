package com.example.tomatocare.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tomatocare.ml.ClassificationResult
import com.example.tomatocare.ml.TomatoClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun AnalysisScreenPreview() {
    AnalysisContent(bitmap = null)
}

private val LeafGreen = Color(0xFF2C6A39)
private val LeafLight = Color(0xFFDBE6D5)
private val Background = Color(0xFFF7F4EB)
private val TextPrimary = Color(0xFF202320)
private val ShadeDark = Color(0xFF141E14)

private const val DOT_COUNT = 8

/**
 * Экран «Анализ»: загружает снимок, в фоне прогоняет его через нейросеть и
 * передаёт результат дальше. Пока идёт обработка, экран просто показывает анимацию.
 *
 * @param imageUri    снимок листа (из камеры или галереи).
 * @param classifierProvider  возвращает общий экземпляр [TomatoClassifier]. Вызывается внутри фоновой
 *                    корутины, поэтому ошибка загрузки модели попадёт в onError, а не уронит приложение.
 * @param modifier    сюда передаётся padding от Scaffold (innerPadding).
 * @param onResult    обработка завершена — сюда приходит результат (переход на ResultScreen).
 * @param onError     не удалось прочитать снимок или классификация упала.
 * @param onCancel    нажатие на «Отмена» и системную кнопку «назад».
 */
@Composable
fun AnalysisScreen(
    imageUri: Uri,
    classifierProvider: () -> TomatoClassifier,
    modifier: Modifier = Modifier,
    onResult: (ClassificationResult) -> Unit = {},
    onError: (Throwable) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val context = LocalContext.current
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    // Свежие версии колбэков без перезапуска LaunchedEffect.
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)

    // Запускается при входе на экран, перезапускается при смене снимка и
    // автоматически отменяется, когда экран закрывается («Отмена» / «назад»).
    LaunchedEffect(imageUri) {
        try {
            val loaded = withContext(Dispatchers.IO) { loadBitmap(context, imageUri) }
            bitmap = loaded

            val result = withContext(Dispatchers.Default) { classifierProvider().classify(loaded) }
            currentOnResult(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e("AnalysisScreen", "Ошибка анализа", e)
            currentOnError(e)
        }
    }

    AnalysisContent(
        bitmap = bitmap,
        modifier = modifier,
        onCancel = onCancel
    )
}

@Composable
private fun AnalysisContent(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {}
) {
    // Во время анализа «назад» должно делать то же, что и «Отмена».
    BackHandler(onBack = onCancel)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        AnalysisPhoto(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            bitmap = bitmap
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Анализируем изображение",
            modifier = Modifier.fillMaxWidth(),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LeafGreen
            ),
            border = BorderStroke(width = 1.5.dp, color = LeafGreen)
        ) {
            Text(
                text = "Отмена",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AnalysisPhoto(
    modifier: Modifier = Modifier,
    bitmap: Bitmap?
) {
    val transition = rememberInfiniteTransition(label = "analysis")

    // 0..1: смещение бегущего затемнения.
    val shade by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shade"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(LeafLight),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Фото листа",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // Перетекающее затемнение поверх фото.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val shift = shade * size.width
                    drawRect(
                        brush = Brush.linearGradient(
                            0f to ShadeDark.copy(alpha = 0.60f),
                            0.5f to ShadeDark.copy(alpha = 0.10f),
                            1f to ShadeDark.copy(alpha = 0.60f),
                            start = Offset(shift, 0f),
                            end = Offset(shift + size.width, 0f),
                            tileMode = TileMode.Repeated
                        )
                    )
                }
        )

        DotsSpinner(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Кружок из 8 точек: яркая «голова» бежит по часовой стрелке, за ней след. */
@Composable
private fun DotsSpinner(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "spinner")

    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "turn"
    )

    Canvas(modifier = modifier.size(64.dp)) {
        val ringRadius = 26.dp.toPx()
        val dotRadius = 5.dp.toPx()
        val head = turn * DOT_COUNT

        for (i in 0 until DOT_COUNT) {
            // Насколько точка отстаёт от «головы»: 0 — яркая, ближе к 1 — почти прозрачная.
            val behind = (((head - i) % DOT_COUNT) + DOT_COUNT) % DOT_COUNT / DOT_COUNT
            val alpha = 1f - 0.85f * behind

            val angle = Math.toRadians(-90.0 + i * 360.0 / DOT_COUNT)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(
                    x = center.x + ringRadius * cos(angle).toFloat(),
                    y = center.y + ringRadius * sin(angle).toFloat()
                )
            )
        }
    }
}

/** Максимальная сторона загружаемого снимка: для экрана и 224×224 модели этого с запасом хватает. */
private const val MAX_SIDE = 1280

/**
 * Читает снимок из [uri] в программный Bitmap (на нём работает getPixel в классификаторе),
 * применяет EXIF-поворот и уменьшает слишком большие фото. Вызывать не на главном потоке.
 */
internal fun loadBitmap(context: Context, uri: Uri): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        // ImageDecoder сам учитывает EXIF-ориентацию.
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val side = max(info.size.width, info.size.height)
            if (side > MAX_SIDE) {
                decoder.setTargetSampleSize(Integer.highestOneBit(side / MAX_SIDE).coerceAtLeast(1))
            }
        }
    }

    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val side = max(bounds.outWidth, bounds.outHeight)

    val options = BitmapFactory.Options().apply {
        inSampleSize = if (side > MAX_SIDE) Integer.highestOneBit(side / MAX_SIDE) else 1
    }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IllegalStateException("Не удалось прочитать изображение: $uri")

    val orientation = resolver.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return decoded
    }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}