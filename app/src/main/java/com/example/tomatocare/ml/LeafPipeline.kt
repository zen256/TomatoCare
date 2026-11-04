package com.example.tomatocare.ml

import android.content.Context
import android.graphics.Bitmap

sealed interface AnalysisOutcome {
    /** YOLO не нашёл ни одного листа. */
    object NoLeaf : AnalysisOutcome

    data class Leaf(
        val crop: CropType,
        val detectionConfidence: Float,
        val classification: ClassificationResult
    ) : AnalysisOutcome
}

class LeafPipeline(context: Context) {

    private val appContext = context.applicationContext
    private var detector: LeafDetector? = null
    private val classifiers = mutableMapOf<CropType, LeafClassifier>()

    @Synchronized
    private fun detector() = detector ?: LeafDetector(appContext).also { detector = it }

    @Synchronized
    private fun classifier(crop: CropType) = classifiers.getOrPut(crop) {
        LeafClassifier(appContext, crop.modelFile, crop.classesFile)
    }

    fun warmUp() {
        detector()
    }

    /** Вызывать не на главном потоке. */
    fun analyze(bitmap: Bitmap): AnalysisOutcome {
        val detection = detector().detect(bitmap) ?: return AnalysisOutcome.NoLeaf
        val leafBitmap = LeafCropper.crop(bitmap, detection)
        val classification = classifier(detection.crop).classify(leafBitmap)
        return AnalysisOutcome.Leaf(detection.crop, detection.confidence, classification)
    }

    @Synchronized
    fun close() {
        detector?.close()
        classifiers.values.forEach { it.close() }
        classifiers.clear()
    }
}