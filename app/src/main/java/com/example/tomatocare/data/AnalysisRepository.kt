package com.example.tomatocare.data

import android.content.Context
import android.net.Uri
import com.example.tomatocare.ml.AnalysisOutcome
import kotlinx.coroutines.flow.Flow

class AnalysisRepository(
    private val context: Context,
    private val dao: AnalysisDao
) {

    val analyses: Flow<List<AnalysisEntity>> = dao.observeAll()

    fun observeById(id: Long): Flow<AnalysisEntity?> {
        return dao.observeById(id)
    }

    suspend fun save(
        uri: Uri,
        outcome: AnalysisOutcome.Leaf
    ): Long {
        val savedImage = ImageStorage.copyFromUri(context, uri)

        try {
            val classification = outcome.classification

            val otherIndices = classification.probabilities.indices
                .sortedByDescending { classification.probabilities[it] }
                .filter {
                    classification.labels.getOrNull(it) != classification.className
                }
                .take(2)

            val alternative1Index = otherIndices.getOrNull(0)
            val alternative2Index = otherIndices.getOrNull(1)

            val entity = AnalysisEntity(
                crop = outcome.crop.name,

                diagnosis = classification.className,
                confidence = classification.confidence,

                alternative1 = alternative1Index?.let {
                    classification.labels.getOrNull(it)
                },
                alternative1Confidence = alternative1Index?.let {
                    classification.probabilities[it]
                },

                alternative2 = alternative2Index?.let {
                    classification.labels.getOrNull(it)
                },
                alternative2Confidence = alternative2Index?.let {
                    classification.probabilities[it]
                },

                createdAt = System.currentTimeMillis(),
                imagePath = savedImage.relativePath
            )

            val id = dao.insert(entity)

            // Удаляем временное фото только после успешной записи в БД.
            ImageStorage.deleteCacheSource(context, uri)

            return id
        } catch (e: Exception) {
            // Удаляем новую копию, если сохранить запись не удалось.
            ImageStorage.delete(context, savedImage.relativePath)
            throw e
        }
    }

    suspend fun delete(id: Long) {
        val analysis = dao.getById(id) ?: return

        dao.deleteById(id)
        ImageStorage.delete(context, analysis.imagePath)
    }
}