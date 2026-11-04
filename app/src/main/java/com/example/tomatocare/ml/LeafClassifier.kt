package com.example.tomatocare.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import java.nio.FloatBuffer
import kotlin.math.exp

data class ClassificationResult(
    val className: String,
    val confidence: Float,
    val probabilities: List<Float>,
    val labels: List<String>
)

class LeafClassifier(
    context: Context,
    modelFile: String,
    classesFile: String
) {
    companion object {
        private const val IMAGE_SIZE = 224
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }

    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    private val classes: List<String>

    init {
        val modelBytes = context.assets.open(modelFile).use { it.readBytes() }
        session = environment.createSession(modelBytes, OrtSession.SessionOptions())
        inputName = session.inputNames.first()

        val json = context.assets.open(classesFile).bufferedReader().use { it.readText() }
        val arr = JSONArray(json)
        classes = List(arr.length()) { arr.getString(it) }
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        val resized = Bitmap.createScaledBitmap(bitmap, IMAGE_SIZE, IMAGE_SIZE, true)
        val plane = IMAGE_SIZE * IMAGE_SIZE
        val pixels = IntArray(plane)
        resized.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        val input = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = pixels[i]
            input[i] = (((p shr 16) and 0xFF) / 255f - MEAN[0]) / STD[0]
            input[plane + i] = (((p shr 8) and 0xFF) / 255f - MEAN[1]) / STD[1]
            input[2 * plane + i] = ((p and 0xFF) / 255f - MEAN[2]) / STD[2]
        }

        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(1, 3, IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())
        )

        val logits = tensor.use {
            session.run(mapOf(inputName to tensor)).use { result ->
                (((result[0].value) as Array<*>)[0] as FloatArray).copyOf()
            }
        }

        val probs = softmax(logits)
        var maxIndex = 0
        for (i in probs.indices) if (probs[i] > probs[maxIndex]) maxIndex = i

        return ClassificationResult(
            className = classes[maxIndex],
            confidence = probs[maxIndex],
            probabilities = probs.toList(),
            labels = classes
        )
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val max = logits.max()
        val e = FloatArray(logits.size) { exp((logits[it] - max).toDouble()).toFloat() }
        val sum = e.sum()
        return FloatArray(e.size) { e[it] / sum }
    }

    fun close() = session.close()
}