package com.example.tomatocare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analyses")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val crop: String,

    val diagnosis: String,
    val confidence: Float,

    val alternative1: String?,
    val alternative1Confidence: Float?,

    val alternative2: String?,
    val alternative2Confidence: Float?,

    val createdAt: Long,

    // Относительный путь внутри filesDir, например photos/abc123.jpg
    val imagePath: String
)