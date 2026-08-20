package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors
import androidx.compose.foundation.Canvas

/**
 * Reemplaza el paquete `signature` de Flutter (`SignatureController` +
 * widget `Signature`). Compose no tiene una librería multiplataforma
 * equivalente lista para usar, así que la captura de trazos se dibuja acá
 * a mano con `Canvas` + `Path` (misma decisión ya anotada en MIGRACION.md
 * para el módulo 3).
 *
 * Diferencia de diseño respecto al original: `onSave(filePath: String)`
 * escribía un PNG a un directorio temporal (`path_provider`), algo que en
 * Kotlin Multiplatform no es 100% portable entre Android/iOS sin
 * `expect`/`actual` de todos modos. Siguiendo la misma decisión que ya se
 * tomó para `SyncService` en el módulo 3 (guardar bytes en vez de rutas de
 * archivo), acá `onSave` recibe directamente los bytes PNG
 * (`ByteArray`) — quien llama decide si los sube, los guarda localmente
 * con `OfflineRepository`, o los encola con `SyncService`.
 */
@Composable
fun SignaturePad(
    onSave: (ByteArray) -> Unit,
    modifier: Modifier = Modifier,
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var currentStroke by remember { mutableStateOf(listOf<Offset>()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val encoder = remember { SignatureEncoder() }
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 3.dp.toPx() }

    val isEmpty = strokes.isEmpty() && currentStroke.size < 2

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .border(BorderStroke(1.dp, AppColors.SurfaceBorder), RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> currentStroke = listOf(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            if (currentStroke.size > 1) strokes = strokes + listOf(currentStroke)
                            currentStroke = emptyList()
                        },
                        onDragCancel = { currentStroke = emptyList() },
                    )
                },
        ) {
            for (stroke in strokes + listOf(currentStroke)) {
                if (stroke.size < 2) continue
                val path = Path()
                path.moveTo(stroke.first().x, stroke.first().y)
                for (point in stroke.drop(1)) path.lineTo(point.x, point.y)
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedButton(onClick = { strokes = emptyList(); currentStroke = emptyList() }) {
                Icon(imageVector = AppIcons.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Limpiar")
            }
            Button(
                onClick = {
                    if (!isEmpty && canvasSize.width > 0 && canvasSize.height > 0) {
                        val finalStrokes = if (currentStroke.size > 1) strokes + listOf(currentStroke) else strokes
                        val bytes = encoder.toPng(finalStrokes, canvasSize.width, canvasSize.height, strokeWidthPx)
                        onSave(bytes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary, contentColor = Color.White),
            ) {
                Icon(imageVector = AppIcons.CheckFilled, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar Firma")
            }
        }
    }
}

/**
 * `expect`/`actual` (mismo patrón que `PdfRenderer`/`ImagePickerService`):
 * cada plataforma "revela" (renderiza) los trazos capturados en Compose a
 * un PNG real usando su motor de dibujo nativo.
 */
expect class SignatureEncoder() {
    fun toPng(strokes: List<List<Offset>>, width: Int, height: Int, strokeWidthPx: Float): ByteArray
}
