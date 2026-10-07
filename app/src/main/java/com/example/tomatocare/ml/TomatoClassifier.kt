package com.example.tomatocare.ml

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.json.JSONArray
import java.nio.FloatBuffer
import kotlin.math.exp

data class ClassificationResult(
    val className: String,
    val confidence: Float,
    val probabilities: List<Float>,
    val labels: List<String>
)

class TomatoClassifier(
    private val context: Context
) {

    companion object {
        private const val IMAGE_SIZE = 224
        private const val MODEL_FILE = "efficientnet_b0.onnx"
        private const val CLASSES_FILE = "classes.json"

        private val MEAN = floatArrayOf(
            0.485f,
            0.456f,
            0.406f
        )

        private val STD = floatArrayOf(
            0.229f,
            0.224f,
            0.225f
        )
    }

    private val environment = OrtEnvironment.getEnvironment()

    private val session: OrtSession

    private val classes: List<String>

    init {
        val modelBytes = context.assets
            .open(MODEL_FILE)
            .use { it.readBytes() }

        session = environment.createSession(
            modelBytes,
            OrtSession.SessionOptions()
        )

        val json = context.assets
            .open(CLASSES_FILE)
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)

        classes = List(jsonArray.length()) { index ->
            jsonArray.getString(index)
        }
    }

    fun classify(bitmap: Bitmap): ClassificationResult {

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            IMAGE_SIZE,
            IMAGE_SIZE,
            true
        )

        val input = FloatArray(
            1 * 3 * IMAGE_SIZE * IMAGE_SIZE
        )

        var index = 0

        for (channel in 0 until 3) {

            for (y in 0 until IMAGE_SIZE) {

                for (x in 0 until IMAGE_SIZE) {

                    val pixel = resizedBitmap.getPixel(x, y)

                    val value = when (channel) {
                        0 -> ((pixel shr 16) and 0xFF) / 255f
                        1 -> ((pixel shr 8) and 0xFF) / 255f
                        else -> (pixel and 0xFF) / 255f
                    }

                    input[index++] =
                        (value - MEAN[channel]) / STD[channel]
                }
            }
        }

        val inputTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(
                1,
                3,
                IMAGE_SIZE.toLong(),
                IMAGE_SIZE.toLong()
            )
        )

        val result = session.run(
            mapOf("input" to inputTensor)
        )

        val output = result[0].value as Array<*>

        val logits = output[0] as FloatArray

        val probabilities = softmax(logits)

        var maxIndex = 0

        for (i in probabilities.indices) {
            if (probabilities[i] > probabilities[maxIndex]) {
                maxIndex = i
            }
        }

        val classificationResult = ClassificationResult(
            className = classes[maxIndex],
            confidence = probabilities[maxIndex],
            probabilities = probabilities.toList(),
            labels = classes
        )

        inputTensor.close()
        result.close()

        return classificationResult
    }

    private fun softmax(logits: FloatArray): FloatArray {

        val maxLogit = logits.maxOrNull() ?: 0f

        val expValues = FloatArray(logits.size)

        var sum = 0.0

        for (i in logits.indices) {

            expValues[i] = exp(
                (logits[i] - maxLogit).toDouble()
            ).toFloat()

            sum += expValues[i]
        }

        for (i in expValues.indices) {
            expValues[i] =
                (expValues[i] / sum).toFloat()
        }

        return expValues
    }

    fun close() {
        session.close()
    }
}