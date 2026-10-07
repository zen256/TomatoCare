package com.example.tomatocare.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.res.painterResource
import com.example.tomatocare.R

import androidx.compose.ui.tooling.preview.Preview

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onCameraClick = {}
    )
}

private val TomatoRed = Color(0xFFB3261D)
private val TomatoDark = Color(0xFFB71C1C)
private val LeafGreen = Color(0xFF2C6A39)
private val LeafLight = Color(0xFFDBE6D5)
private val Background = Color(0xFFF7F4EB)
private val TextPrimary = Color(0xFF202320)
private val TextSecondary = Color(0xFF6B716B)
private val CardBorder = Color(0xFFE3E6E3)

@Composable
fun HomeScreen(
    onCameraClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp) //vertical = 20.dp
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TomatoCare",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = TomatoRed
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Определите болезнь томата\nпо фото листа",
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        LeafIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TomatoRed
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = "Сделать фото",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                // Выбор изображения из галереи будет подключён следующим шагом.
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LeafGreen
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = LeafGreen
            )
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = "Выбрать из галереи",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Последний анализ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        LastAnalysisCard(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LeafIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
        .size(210.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(LeafLight),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.leaf),
            contentDescription = "Лист томата",
            modifier = Modifier
                .widthIn(120.dp)
                .height(120.dp)
        )
    }
}

@Composable
private fun LastAnalysisCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LeafLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFlorist,
                    contentDescription = null,
                    tint = LeafGreen,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Пока нет анализов",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.size(5.dp))

                    Text(
                        text = "История анализов появится здесь",
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}