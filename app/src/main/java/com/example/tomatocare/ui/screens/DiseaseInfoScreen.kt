package com.example.tomatocare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DiseaseInfoScreenPreview() {
    DiseaseInfoScreen(
        diseaseName = "Common_rust",
        onBackClick = {}
    )
}

private data class DiseaseInfo(
    val russianName: String,
    val description: String
)

private val diseaseInfo = mapOf(
    "Common_rust" to DiseaseInfo(
        "Обыкновенная ржавчина",
        "Грибковое заболевание кукурузы. На листьях появляются небольшие выпуклые пустулы оранжево-коричневого цвета. Со временем они могут распространяться по всей поверхности листа, вызывая его преждевременное высыхание."
    ),
    "Gray_leaf_spot" to DiseaseInfo(
        "Серая пятнистость листьев",
        "Грибковое заболевание кукурузы. На листьях образуются продолговатые серые или коричневатые пятна, ограниченные жилками листа. При сильном поражении пятна сливаются, а листья постепенно отмирают."
    ),
    "Northern_Leaf_Blight" to DiseaseInfo(
        "Северная пятнистость (гельминтоспориоз)",
        "Грибковое заболевание кукурузы. Характеризуется появлением крупных продолговатых серо-зелёных или коричневых пятен на листьях. Поражённые участки постепенно увеличиваются, из-за чего листовая поверхность высыхает."
    ),
    "Bacterial_spot" to DiseaseInfo(
        "Бактериальная пятнистость",
        "Бактериальное заболевание томата. На листьях появляются небольшие тёмные пятна, иногда окружённые желтоватой каймой. При сильном поражении пятна увеличиваются и сливаются, повреждённые участки листьев отмирают."
    ),
    "Early_blight" to DiseaseInfo(
        "Альтернариоз (ранняя пятнистость)",
        "Грибковое заболевание томата. На листьях появляются коричневые пятна с характерными концентрическими кольцами. Обычно поражение начинается с нижних листьев и постепенно распространяется вверх."
    ),
    "Late_blight" to DiseaseInfo(
        "Фитофтороз",
        "Опасное заболевание томата, вызываемое оомицетом Phytophthora infestans. На листьях появляются крупные бурые пятна, которые быстро увеличиваются при высокой влажности. Поражённые ткани могут загнивать."
    ),
    "Leaf_Mold" to DiseaseInfo(
        "Кладоспориоз (бурая пятнистость)",
        "Грибковое заболевание томата. На верхней стороне листьев появляются желтоватые пятна, а на нижней — оливково-зелёный или сероватый налёт. При развитии заболевания листья засыхают и опадают."
    ),
    "Septoria_leaf_spot" to DiseaseInfo(
        "Септориоз (белая пятнистость)",
        "Грибковое заболевание томата. На листьях появляются многочисленные небольшие округлые пятна с тёмной каймой и светлой сероватой серединой. Чаще всего первыми поражаются нижние листья."
    ),
    "Tomato_mosaic_virus" to DiseaseInfo(
        "Вирус мозаики томата",
        "Вирусное заболевание, вызывающее характерную мозаичную окраску листьев: чередование светло- и тёмно-зелёных участков. Листья могут деформироваться, а рост растения замедляется."
    ),
    "brown_rust" to DiseaseInfo(
        "Бурая ржавчина",
        "Грибковое заболевание пшеницы. На листьях образуются многочисленные мелкие оранжево-коричневые пустулы, содержащие споры гриба. При сильном поражении листья преждевременно высыхают."
    ),
    "mildew" to DiseaseInfo(
        "Мучнистая роса",
        "Грибковое заболевание пшеницы. На поверхности листьев появляется белый мучнистый налёт, который со временем может уплотняться и приобретать сероватый оттенок. Поражённые листья постепенно желтеют и отмирают."
    ),
    "yellow_rust" to DiseaseInfo(
        "Жёлтая ржавчина",
        "Грибковое заболевание пшеницы. На листьях появляются мелкие жёлтые пустулы, расположенные характерными продольными полосами вдоль жилок. При сильном поражении листовая поверхность постепенно отмирает."
    ),
    "healthy" to DiseaseInfo(
        "Здоровый лист",
        "На фотографии не обнаружено характерных признаков заболеваний. Лист выглядит здоровым. Продолжайте наблюдать за состоянием растения и регулярно осматривайте листья."
    )
)

@Composable
fun DiseaseInfoScreen(
    diseaseName: String,
    onBackClick: () -> Unit
) {
    val info = diseaseInfo[diseaseName]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F4EB))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(25.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color(0xFF202320)
                )
            }

            Spacer(modifier = Modifier.size(4.dp))

            Text(
                text = "О болезни",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF202320)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = info?.russianName ?: "Неизвестное заболевание",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB3261D)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = diseaseName.replace("_", " "),
            fontSize = 15.sp,
            color = Color(0xFF6B716B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = info?.description
                ?: "Описание для данного заболевания пока отсутствует.",
            fontSize = 16.sp,
            lineHeight = 25.sp,
            color = Color(0xFF202320)
        )
    }
}