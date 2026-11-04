
package com.example.tomatocare.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tomatocare.R
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip

private val TomatoRed = Color(0xFFE53935)
private val Background = Color(0xFFF7F4EB)
private val CardBackground = Color.White
private val TextColor = Color(0xFF333333)
private val SecondaryText = Color(0xFF757575)

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                )
            }

            packageInfo.versionName ?: "Неизвестно"
        } catch (e: Exception) {
            "Неизвестно"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(15.dp))
        // Информация о приложении
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = "Иконка AgroScan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    modifier = Modifier.padding(start = 18.dp)
                ) {
                    Text(
                        text = "AgroScan",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TomatoRed
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Версия $versionName",
                        fontSize = 14.sp,
                        color = SecondaryText
                    )
                }
            }
        }

        // Назначение приложения
        AboutSection(title = "О приложении") {
            Text(
                text = "AgroScan — приложение для определения " +
                        "заболеваний сельскохозяйственных культур " +
                        "по фотографиям листьев.",
                fontSize = 15.sp,
                color = TextColor,
                lineHeight = 23.sp
            )
        }

        // Разработчики
        AboutSection(title = "Разработчики") {
            Text(
                text = "AgroScan — проект, посвящённый " +
                        "применению технологий машинного обучения " +
                        "для распознавания заболеваний растений.",
                fontSize = 15.sp,
                color = TextColor,
                lineHeight = 23.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Над проектом работали: \nТрошин И.Д.\nЗаседатель Н.С.\nАндреюк М.О.\nЖук Б.Д.",
                fontSize = 14.sp,
                color = SecondaryText
            )

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        uriHandler.openUri(
                            "https://github.com/zen256/AgroScan"
                        )
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GitHub",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TomatoRed
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Открыть репозиторий ↗",
                    fontSize = 14.sp,
                    color = SecondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TomatoRed
            )

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}
