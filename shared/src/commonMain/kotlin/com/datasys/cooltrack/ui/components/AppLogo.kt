package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppBranding
import com.datasys.cooltrack.core.AppGradients
import com.datasys.cooltrack.core.AppLogoMark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Logo de CoolTrack — un único punto de definición, fácil de cambiar más
 * adelante (editar este archivo, o proveer un [LocalAppLogo] distinto, o
 * elegir otra [AppLogoMark] desde Ajustes).
 *
 * Se dibuja con Compose (vectorial, multiplataforma, escala sin pérdida).
 * Para reemplazarlo por un SVG/imagen propia: sustituir el cuerpo de
 * [AppLogoGlyph] por un `Image(painterResource(...))`.
 */

/** Permite sobrescribir el glifo del logo para toda una parte del árbol. */
val LocalAppLogo: ProvidableCompositionLocal<(@Composable (Modifier, Color) -> Unit)?> =
    compositionLocalOf { null }

@Composable
fun ProvideAppLogo(glyph: @Composable (Modifier, Color) -> Unit, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLogo provides glyph, content = content)
}

/**
 * Logo "de app": el glifo dentro de una placa redondeada con el gradiente de
 * marca (el look del ícono de launcher / splash / login).
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    corner: Dp = 26.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(AppGradients.hero()),
        contentAlignment = Alignment.Center,
    ) {
        AppLogoGlyph(Modifier.size(size * 0.52f), Color.White)
    }
}

/** Solo el glifo del logo, en el color dado (para headers, badges, etc.). */
@Composable
fun AppLogoGlyph(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    mark: AppLogoMark = AppBranding.logo,
) {
    val override = LocalAppLogo.current
    if (override != null) {
        override(modifier, color)
        return
    }
    Canvas(modifier) {
        when (mark) {
            AppLogoMark.SNOWFLAKE -> drawSnowflake(color)
            AppLogoMark.FAN -> drawFan(color)
            AppLogoMark.DROPLET -> drawDroplet(color)
            AppLogoMark.HEXAGON -> drawHexagon(color)
            AppLogoMark.BOLT -> drawBolt(color)
        }
    }
}

// --- glifos ---------------------------------------------------------------

private fun DrawScope.drawSnowflake(color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f
    val stroke = Stroke(width = r * 0.16f, cap = StrokeCap.Round)
    repeat(6) { i ->
        rotate(degrees = 60f * i, pivot = c) {
            drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), stroke.width, stroke.cap)
            // ramitas
            drawLine(color, Offset(c.x, c.y - r * 0.55f), Offset(c.x - r * 0.32f, c.y - r * 0.9f), stroke.width, stroke.cap)
            drawLine(color, Offset(c.x, c.y - r * 0.55f), Offset(c.x + r * 0.32f, c.y - r * 0.9f), stroke.width, stroke.cap)
        }
    }
    drawCircle(color, r * 0.16f, c)
}

private fun DrawScope.drawFan(color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f
    repeat(3) { i ->
        rotate(degrees = 120f * i, pivot = c) {
            val p = Path().apply {
                moveTo(c.x, c.y)
                quadraticBezierTo(c.x - r * 0.9f, c.y - r * 0.2f, c.x - r * 0.15f, c.y - r * 0.95f)
                quadraticBezierTo(c.x + r * 0.15f, c.y - r * 0.55f, c.x, c.y)
                close()
            }
            drawPath(p, color)
        }
    }
    drawCircle(color, r * 0.18f, c)
}

private fun DrawScope.drawDroplet(color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f
    val p = Path().apply {
        moveTo(c.x, c.y - r)
        cubicTo(c.x + r * 0.95f, c.y - r * 0.1f, c.x + r * 0.7f, c.y + r * 0.95f, c.x, c.y + r * 0.95f)
        cubicTo(c.x - r * 0.7f, c.y + r * 0.95f, c.x - r * 0.95f, c.y - r * 0.1f, c.x, c.y - r)
        close()
    }
    drawPath(p, color)
    drawCircle(Color.White.copy(alpha = 0.28f), r * 0.22f, Offset(c.x - r * 0.28f, c.y + r * 0.1f))
}

private fun DrawScope.drawHexagon(color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f
    val p = Path()
    for (i in 0..5) {
        val a = (PI / 3 * i - PI / 6)
        val x = c.x + r * cos(a).toFloat()
        val y = c.y + r * sin(a).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color, style = Stroke(width = r * 0.2f))
    drawCircle(color, r * 0.28f, c)
}

private fun DrawScope.drawBolt(color: Color) {
    val c = center
    val r = min(size.width, size.height) / 2f
    val p = Path().apply {
        moveTo(c.x + r * 0.15f, c.y - r)
        lineTo(c.x - r * 0.55f, c.y + r * 0.12f)
        lineTo(c.x + r * 0.02f, c.y + r * 0.12f)
        lineTo(c.x - r * 0.15f, c.y + r)
        lineTo(c.x + r * 0.55f, c.y - r * 0.18f)
        lineTo(c.x - r * 0.02f, c.y - r * 0.18f)
        close()
    }
    drawPath(p, color)
}
