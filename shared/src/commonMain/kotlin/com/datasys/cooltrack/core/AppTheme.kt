package com.datasys.cooltrack.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paleta "Arctic" de CoolTrack: azules y cian fríos (coherentes con el
 * negocio — mantenimiento de aires acondicionados / HVAC) sobre superficies
 * neutras, con acentos semánticos (éxito/alerta/error) desaturados para no
 * competir con la marca. Reemplaza a la paleta anterior (navy plano + un
 * solo cian) en `AppColors`, manteniendo los mismos nombres de campo para
 * no romper referencias existentes en el resto de la UI.
 */
object AppColors {
    // Marca — "ink" profundo usado en headers, botones secundarios y modo oscuro.
    val Primary = Color(0xFF0B1E3D)
    val PrimaryLight = Color(0xFF16305C)
    val PrimaryDark = Color(0xFF060F20)

    // Marca — acento cian/cielo, color de acción principal en toda la app.
    val Secondary = Color(0xFF0EA5E9)
    val SecondaryLight = Color(0xFF38BDF8)
    val SecondaryDark = Color(0xFF0284C7)

    // Acento terciario (hielo) para gradientes y detalles decorativos.
    val Accent = Color(0xFF06B6D4)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFE11D48)
    val Info = Color(0xFF0EA5E9)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceBorder = Color(0xFFE2E8F0)
    val Outline = Color(0xFF94A3B8)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextMuted = Color(0xFF94A3B8)

    // Dark mode
    val DarkBackground = Color(0xFF070B14)
    val DarkSurface = Color(0xFF111827)
    val DarkSurfaceAlt = Color(0xFF16213A)
    val DarkBorder = Color(0xFF263449)

    val StatusPending = Color(0xFFF59E0B)
    val StatusAssigned = Color(0xFF2563EB)
    val StatusAccepted = Color(0xFF7C3AED)
    val StatusInTransit = Color(0xFF06B6D4)
    val StatusInProgress = Color(0xFF4F46E5)
    val StatusCompleted = Color(0xFF10B981)
    val StatusCancelled = Color(0xFFE11D48)

    val QuoteDraft = Color(0xFF64748B)
    val QuoteSent = Color(0xFF2563EB)
    val QuoteApproved = Color(0xFF10B981)
    val QuoteRejected = Color(0xFFE11D48)
    val QuoteExpired = Color(0xFFF59E0B)

    /** Mapea OrderStatus -> color, usado por AppStatusBadge para pintar chips de estado. */
    fun forOrderStatus(status: OrderStatus): Color = when (status) {
        OrderStatus.PENDING -> StatusPending
        OrderStatus.ASSIGNED -> StatusAssigned
        OrderStatus.ACCEPTED -> StatusAccepted
        OrderStatus.IN_TRANSIT -> StatusInTransit
        OrderStatus.IN_PROGRESS -> StatusInProgress
        OrderStatus.COMPLETED -> StatusCompleted
        OrderStatus.CANCELLED -> StatusCancelled
    }

    /** Mapea QuoteStatus -> color, usado por AppQuoteStatusBadge. */
    fun forQuoteStatus(status: QuoteStatus): Color = when (status) {
        QuoteStatus.DRAFT -> QuoteDraft
        QuoteStatus.SENT -> QuoteSent
        QuoteStatus.APPROVED -> QuoteApproved
        QuoteStatus.REJECTED -> QuoteRejected
        QuoteStatus.EXPIRED -> QuoteExpired
    }
}

private val LightColors = lightColorScheme(
    primary = AppColors.Secondary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = AppColors.SecondaryDark,
    secondary = AppColors.Primary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = AppColors.Primary,
    tertiary = AppColors.Accent,
    onTertiary = Color.White,
    background = AppColors.SurfaceVariant,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.SurfaceBorder,
    outlineVariant = AppColors.SurfaceBorder,
    error = AppColors.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFCE7EC),
    onErrorContainer = AppColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.SecondaryLight,
    onPrimary = AppColors.DarkBackground,
    primaryContainer = AppColors.SecondaryDark,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = AppColors.SecondaryLight,
    onSecondary = AppColors.DarkBackground,
    secondaryContainer = AppColors.DarkSurfaceAlt,
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = AppColors.Accent,
    onTertiary = AppColors.DarkBackground,
    background = AppColors.DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = AppColors.DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = AppColors.DarkSurfaceAlt,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = AppColors.DarkBorder,
    outlineVariant = AppColors.DarkBorder,
    error = Color(0xFFFB7185),
    onError = AppColors.DarkBackground,
    errorContainer = Color(0xFF4C0519),
    onErrorContainer = Color(0xFFFECDD3),
)

/** Escala tipográfica Material3 completa sobre Plus Jakarta Sans (marca CoolTrack). */
@Composable
private fun appTypography(): Typography {
    val font = appFontFamily()
    return Typography(
        displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
        displaySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
        headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
        bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.15.sp),
        labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp),
    )
}

/** Escala de esquinas redondeadas de marca — más suave que el 12dp plano anterior. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Equivalente a AppTheme.light / AppTheme.dark, aplicado con MaterialTheme (Compose Multiplatform). */
@Composable
fun CooltrackTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = appTypography(),
        shapes = AppShapes,
        content = content,
    )
}
