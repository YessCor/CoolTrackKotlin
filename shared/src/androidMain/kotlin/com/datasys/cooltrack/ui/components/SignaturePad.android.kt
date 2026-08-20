package com.datasys.cooltrack.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

/**
 * Implementación Android de SignatureEncoder usando `android.graphics`
 * (`Bitmap` + `Canvas` + `Path`), el mismo motor que ya usa
 * `PdfRenderer.android.kt` para dibujar. Fondo blanco + trazo negro,
 * igual que `SignatureController(penColor: Colors.black,
 * exportBackgroundColor: Colors.white)` en el original.
 */
actual class SignatureEncoder actual constructor() {
    actual fun toPng(strokes: List<List<Offset>>, width: Int, height: Int, strokeWidthPx: Float): ByteArray {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (stroke in strokes) {
            if (stroke.size < 2) continue
            val path = Path()
            path.moveTo(stroke.first().x, stroke.first().y)
            for (point in stroke.drop(1)) path.lineTo(point.x, point.y)
            canvas.drawPath(path, paint)
        }

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        return output.toByteArray()
    }
}
