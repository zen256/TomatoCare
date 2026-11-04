package com.example.tomatocare.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object LeafCropper {
    private const val MAX_CROP_SIDE = 1024f
    private const val MARGIN = 1.05f   // небольшой запас по краям бокса

    fun crop(src: Bitmap, d: LeafDetection): Bitmap {
        val w = d.width * MARGIN
        val h = d.height * MARGIN
        val s = min(1f, MAX_CROP_SIDE / max(w, h))
        val outW = max(1, (w * s).roundToInt())
        val outH = max(1, (h * s).roundToInt())

        val matrix = Matrix().apply {
            postTranslate(-d.cx, -d.cy)
            postRotate(-Math.toDegrees(d.angleRad.toDouble()).toFloat())
            postScale(s, s)
            postTranslate(outW / 2f, outH / 2f)
        }

        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(out).apply {
            drawColor(Color.BLACK)
            drawBitmap(src, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        return out
    }
}