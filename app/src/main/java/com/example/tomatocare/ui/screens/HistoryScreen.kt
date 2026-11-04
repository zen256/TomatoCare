package com.example.tomatocare.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import com.example.tomatocare.TomatoViewModel
import com.example.tomatocare.data.AnalysisEntity
import com.example.tomatocare.data.ImageStorage
import com.example.tomatocare.ml.CropType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Background = Color(0xFFF7F4EB)
private val TomatoRed = Color(0xFFB3261D)
private val TextPrimary = Color(0xFF202320)
private val TextSecondary = Color(0xFF6B716B)
private val LeafLight = Color(0xFFDBE6D5)

@Composable
fun HistoryScreen(
    viewModel: TomatoViewModel,
    onAnalysisClick: (Long) -> Unit
) {
    val analyses by viewModel.analyses.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "История анализов",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 24.dp, bottom = 18.dp)
        )

        if (analyses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Здесь пока нет анализов",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = analyses,
                    key = { it.id }
                ) { analysis ->
                    HistoryItem(
                        analysis = analysis,
                        onClick = { onAnalysisClick(analysis.id) },
                        onDelete = { viewModel.deleteAnalysis(analysis.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    analysis: AnalysisEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val crop = runCatching {
        CropType.valueOf(analysis.crop).displayName
    }.getOrDefault(analysis.crop)

    val date = rememberDate(analysis.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryPhoto(
                imagePath = analysis.imagePath,
                modifier = Modifier.size(76.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = date,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = crop,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = analysis.diagnosis.toDisplayName(
                        CropType.valueOf(analysis.crop)
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TomatoRed
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить анализ",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun HistoryPhoto(
    imagePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        imagePath
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                BitmapFactory.decodeFile(
                    ImageStorage.getFile(context, imagePath).absolutePath
                )
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(LeafLight),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Фото листа",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun rememberDate(timestamp: Long): String {
    return java.text.SimpleDateFormat(
        "dd.MM.yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}