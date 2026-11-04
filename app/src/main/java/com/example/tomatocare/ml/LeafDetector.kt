package com.example.tomatocare.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import java.nio.FloatBuffer
import kotlin.math.min

/** Координаты — в пикселях исходного bitmap. Угол — в радианах. */
data class LeafDetection(
    val crop: CropType,
    val confidence: Float,
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float,
    val angleRad: Float
)

class LeafDetector(context: Context) {

    companion object {
        private const val TAG = "LeafDetector"
        private const val MODEL_FILE = "leaf_detector.onnx"
        private const val INPUT_SIZE = 640
        private const val CONF_THRESHOLD = 0.25f
        private const val PAD_GRAY = 114
    }

    private val environment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String

    init {
        val bytes = context.assets.open(MODEL_FILE).use { it.readBytes() }
        session = environment.createSession(bytes, OrtSession.SessionOptions())
        inputName = session.inputNames.first()
        Log.d(TAG, "outputs: " + session.outputInfo.entries.joinToString { "${it.key}=${it.value.info}" })
    }

    private class Candidate(
        val cx: Float, val cy: Float, val w: Float, val h: Float,
        val conf: Float, val cls: Int, val angle: Float
    )

    fun detect(bitmap: Bitmap): LeafDetection? {
        val scale = min(INPUT_SIZE.toFloat() / bitmap.width, INPUT_SIZE.toFloat() / bitmap.height)
        val padX = (INPUT_SIZE - bitmap.width * scale) / 2f
        val padY = (INPUT_SIZE - bitmap.height * scale) / 2f

        val letterboxed = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(Color.rgb(PAD_GRAY, PAD_GRAY, PAD_GRAY))
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(padX, padY)
        }
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

        val plane = INPUT_SIZE * INPUT_SIZE
        val pixels = IntArray(plane)
        letterboxed.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        letterboxed.recycle()

        val input = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = pixels[i]
            input[i] = ((p shr 16) and 0xFF) / 255f
            input[plane + i] = ((p shr 8) and 0xFF) / 255f
            input[2 * plane + i] = (p and 0xFF) / 255f
        }

        val tensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(input),
            longArrayOf(1, 3, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        )

        tensor.use {
            session.run(mapOf(inputName to tensor)).use { result ->
                val out = result[0] as OnnxTensor
                val shape = out.info.shape
                @Suppress("UNCHECKED_CAST")
                val data = (out.value as Array<Array<FloatArray>>)[0]

                val best = if (shape.size == 3 && shape[2] == 7L) {
                    parseEndToEnd(data)
                } else {
                    parseRaw(data)
                } ?: return null

                val crop = CropType.fromYoloClass(best.cls) ?: return null
                return LeafDetection(
                    crop = crop,
                    confidence = best.conf,
                    cx = (best.cx - padX) / scale,
                    cy = (best.cy - padY) / scale,
                    width = best.w / scale,
                    height = best.h / scale,
                    angleRad = best.angle
                )
            }
        }
    }
    private fun parseEndToEnd(rows: Array<FloatArray>): Candidate? {
        var best: Candidate? = null
        for (r in rows) {
            val conf = r[4]
            if (conf >= CONF_THRESHOLD && (best == null || conf > best.conf)) {
                best = Candidate(r[0], r[1], r[2], r[3], conf, r[5].toInt(), r[6])
            }
        }
        return best
    }
    private fun parseRaw(ch: Array<FloatArray>): Candidate? {
        val nc = ch.size - 5
        val anchors = ch[0].size
        var best: Candidate? = null
        for (i in 0 until anchors) {
            var cls = 0
            var score = ch[4][i]
            for (c in 1 until nc) {
                if (ch[4 + c][i] > score) { score = ch[4 + c][i]; cls = c }
            }
            if (score >= CONF_THRESHOLD && (best == null || score > best.conf)) {
                best = Candidate(ch[0][i], ch[1][i], ch[2][i], ch[3][i], score, cls, ch[4 + nc][i])
            }
        }
        return best
    }

    fun close() = session.close()
}