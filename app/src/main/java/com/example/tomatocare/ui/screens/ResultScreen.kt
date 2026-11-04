
package com.example.tomatocare.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tomatocare.TomatoViewModel
import com.example.tomatocare.data.ImageStorage
import com.example.tomatocare.ml.AnalysisOutcome
import com.example.tomatocare.ml.ClassificationResult
import com.example.tomatocare.ml.CropType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val TomatoRed = Color(0xFFB3261D)
private val LeafGreen = Color(0xFF2C6A39)
private val LeafLight = Color(0xFFDBE6D5)
private val Background = Color(0xFFF7F4EB)
private val TextPrimary = Color(0xFF202320)
private val TextSecondary = Color(0xFF6B716B)
private val BarTrack = Color(0xFFE4DFD0)

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun ResultScreenPreview() {
    ResultContent(
        outcome = AnalysisOutcome.Leaf(
            crop = CropType.TOMATO,
            detectionConfidence = 0.95f,
            classification = ClassificationResult(
                className = "Late_blight",
                confidence = 0.92f,
                probabilities = listOf(0.92f, 0.05f, 0.02f),
                labels = listOf("Late_blight", "Early_blight", "Leaf_Mold")
            )
        ),
        bitmap = null,
        onDetailsClick = {}
    )
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun ResultScreenNoLeafPreview() {
    NoLeafContent(bitmap = null)
}

/**
 * Экран результата сохранённого анализа.
 *
 * Получает только ID записи и загружает остальные данные из Room.
 */
@Composable
fun ResultScreen(
    analysisId: Long,
    viewModel: TomatoViewModel,
    modifier: Modifier = Modifier,
    onDetailsClick: (String) -> Unit,
    onNewPhoto: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val entityFlow = remember(analysisId) {
        viewModel.observeAnalysis(analysisId)
    }

    val entity by entityFlow.collectAsState(initial = null)

    if (entity == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Загрузка результата...",
                color = TextSecondary
            )
        }
        return
    }

    val analysis = entity!!

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        analysis.imagePath
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(
                    ImageStorage.getFile(
                        context,
                        analysis.imagePath
                    ).absolutePath
                )
            }.getOrNull()
        }
    }

    val crop = CropType.valueOf(analysis.crop)

    val labels = buildList {
        add(analysis.diagnosis)
        analysis.alternative1?.let(::add)
        analysis.alternative2?.let(::add)
    }

    val probabilities = buildList {
        add(analysis.confidence)
        analysis.alternative1Confidence?.let(::add)
        analysis.alternative2Confidence?.let(::add)
    }

    val outcome = AnalysisOutcome.Leaf(
        crop = crop,
        detectionConfidence = 0f,
        classification = ClassificationResult(
            className = analysis.diagnosis,
            confidence = analysis.confidence,
            probabilities = probabilities,
            labels = labels
        )
    )

    ResultContent(
        outcome = outcome,
        bitmap = bitmap,
        modifier = modifier,
        onDetailsClick = { diseaseName -> onDetailsClick(diseaseName) },
        onNewPhoto = onNewPhoto,
        onBack = onBack
    )
}

/**
 * Экран для результата, когда YOLO не обнаружила лист.
 * Такой результат не сохраняется в Room.
 */
@Composable
fun NoLeafResultScreen(
    imageUri: Uri?,
    modifier: Modifier = Modifier,
    onNewPhoto: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        imageUri
    ) {
        value = if (imageUri == null) {
            null
        } else {
            runCatching {
                withContext(Dispatchers.IO) {
                    loadBitmap(context, imageUri)
                }
            }.getOrNull()
        }
    }

    NoLeafContent(
        bitmap = bitmap,
        modifier = modifier,
        onNewPhoto = onNewPhoto,
        onBack = onBack
    )
}

@Composable
private fun ResultContent(
    outcome: AnalysisOutcome.Leaf,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onDetailsClick: (String) -> Unit = {},
    onNewPhoto: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val result = outcome.classification
    val crop = outcome.crop

    // Два самых вероятных класса помимо главного.
    val others = result.probabilities.indices
        .sortedByDescending { result.probabilities[it] }
        .filter { result.labels.getOrNull(it) != result.className }
        .take(2)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        ResultHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            PhotoBox(bitmap = bitmap)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Культура: ${crop.displayName}",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Наиболее вероятный диагноз",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = result.className.toDisplayName(crop),
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Bold,
                color = TomatoRed
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Уверенность модели: ${result.confidence.toPercent()}",
                fontSize = 15.sp,
                color = TextPrimary
            )

            if (others.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Другие варианты",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                others.forEach { index ->
                    Spacer(modifier = Modifier.height(10.dp))

                    ProbabilityBar(
                        label = (
                                result.labels.getOrNull(index)
                                    ?: "Класс ${index + 1}"
                                ).toDisplayName(crop),
                        probability = result.probabilities[index]
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Для здорового листа подробностей о болезни нет.
        if (result.className != "healthy") {
            Button(
                onClick = {
                    onDetailsClick(result.className)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TomatoRed
                )
            ) {
                Text(
                    text = "Подробнее о болезни",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = onNewPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LeafGreen
            ),
            border = BorderStroke(
                width = 1.5.dp,
                color = LeafGreen
            )
        ) {
            Text(
                text = "Новый снимок",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Экран для случая, когда детектор не нашёл на снимке ни одного листа. */
@Composable
private fun NoLeafContent(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onNewPhoto: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        ResultHeader(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            PhotoBox(bitmap = bitmap)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Листья не обнаружены",
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Bold,
                color = TomatoRed
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "На фото не удалось найти лист пшеницы, томата или кукурузы. " +
                        "Сделайте новый снимок: один лист крупным планом, при хорошем освещении " +
                        "и без сильных бликов.",
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onNewPhoto,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TomatoRed
            )
        ) {
            Text(
                text = "Новый снимок",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ResultHeader(onBack: () -> Unit) {
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
            text = "Результат",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun PhotoBox(bitmap: Bitmap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(LeafLight)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Фото листа",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ProbabilityBar(
    label: String,
    probability: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextPrimary
            )

            Text(
                text = probability.toPercent(),
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BarTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(probability.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(LeafGreen)
            )
        }
    }
}

private fun Float.toPercent(): String =
    "${(this * 100).roundToInt()}%"

/** Название класса из <culture>_classes.json для показа пользователю. */
internal fun String.toDisplayName(crop: CropType): String = when (crop) {
    CropType.TOMATO -> when (this) {
        "Bacterial_spot" -> "Бактериальная пятнистость"
        "Early_blight" -> "Альтернариоз (ранняя пятнистость)"
        "Late_blight" -> "Фитофтороз"
        "Leaf_Mold" -> "Кладоспориоз (бурая пятнистость)"
        "Septoria_leaf_spot" -> "Септориоз (белая пятнистость)"
        "Tomato_mosaic_virus" -> "Вирус мозаики томата"
        "healthy" -> "Здоровый лист"
        else -> replace('_', ' ')
    }

    CropType.WHEAT -> when (this) {
        "brown_rust" -> "Бурая ржавчина"
        "yellow_rust" -> "Жёлтая ржавчина"
        "mildew" -> "Мучнистая роса"
        "healthy" -> "Здоровый лист"
        else -> replace('_', ' ')
    }

    CropType.CORN -> when (this) {
        "Common_rust" -> "Обыкновенная ржавчина"
        "Gray_leaf_spot" -> "Серая пятнистость листьев"
        "Northern_Leaf_Blight" -> "Северная пятнистость (гельминтоспориоз)"
        "healthy" -> "Здоровый лист"
        else -> replace('_', ' ')
    }
}
