package com.datasys.cooltrack.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paleta "Indigo Frost" de CoolTrack (rediseño total 2026): índigo profundo
 * como color de marca y de acción, navy‑índigo para los headers heroicos,
 * y un cian mínimo reservado para detalles de "hielo" (identidad HVAC).
 * Las superficies llevan un tinte índigo apenas perceptible para que toda
 * la app se sienta parte del mismo sistema.
 *
 * Se mantienen los mismos nombres de campo que la paleta anterior para no
 * romper referencias; solo cambian los valores.
 */
object AppColors {
    // Marca — navy‑índigo profundo: headers heroicos, modo oscuro, botones "secondary".
    val Primary = Color(0xFF1E1B4B)
    val PrimaryLight = Color(0xFF312E81)
    val PrimaryDark = Color(0xFF14122E)

    // Marca — índigo: el color de acción principal en toda la app.
    val Secondary = Color(0xFF4F46E5)
    val SecondaryLight = Color(0xFF6366F1)
    val SecondaryDark = Color(0xFF4338CA)

    // Acento cian (hielo) — SOLO para gradientes y micro‑detalles decorativos.
    val Accent = Color(0xFF22D3EE)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFF43F5E)
    val Info = Color(0xFF4F46E5)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF4F4FB)
    val SurfaceBorder = Color(0xFFE7E7F4)
    val Outline = Color(0xFF9C99B8)

    // Tinte de fondo de pantalla (ligeramente más frío que blanco puro).
    val Background = Color(0xFFF6F6FC)

    val TextPrimary = Color(0xFF1B1930)
    val TextSecondary = Color(0xFF56526E)
    val TextMuted = Color(0xFF908CA8)

    // Sombra teñida de índigo — da "peso" a las tarjetas sin verse gris sucio.
    val ShadowTint = Color(0x1A4F46E5)

    // Dark mode
    val DarkBackground = Color(0xFF0B0A1A)
    val DarkSurface = Color(0xFF16142B)
    val DarkSurfaceAlt = Color(0xFF201D3D)
    val DarkBorder = Color(0xFF322E52)

    val StatusPending = Color(0xFFF59E0B)
    val StatusAssigned = Color(0xFF4F46E5)
    val StatusAccepted = Color(0xFF7C3AED)
    val StatusInTransit = Color(0xFF06B6D4)
    val StatusInProgress = Color(0xFF6366F1)
    val StatusCompleted = Color(0xFF10B981)
    val StatusCancelled = Color(0xFFF43F5E)

    val QuoteDraft = Color(0xFF7C7A99)
    val QuoteSent = Color(0xFF4F46E5)
    val QuoteApproved = Color(0xFF10B981)
    val QuoteRejected = Color(0xFFF43F5E)
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

/**
 * Gradientes de marca. El "hero" (navy‑índigo -> índigo -> cian) es la firma
 * visual del rediseño: aparece en headers de pantalla, el splash y el login.
 */
object AppGradients {
    val heroColors = listOf(Color(0xFF1E1B4B), Color(0xFF4F46E5), Color(0xFF22D3EE))
    val heroSoftColors = listOf(Color(0xFF312E81), Color(0xFF4F46E5))
    val accentColors = listOf(Color(0xFF6366F1), Color(0xFF22D3EE))

    fun hero(): Brush = Brush.linearGradient(heroColors)
    fun heroVertical(): Brush = Brush.verticalGradient(heroColors)
    fun heroSoft(): Brush = Brush.linearGradient(heroSoftColors)
    fun accent(): Brush = Brush.linearGradient(accentColors)

    /** Halo radial tenue para fondos "premium" detrás de contenido claro. */
    fun halo(): Brush = Brush.radialGradient(
        colors = listOf(Color(0x2E4F46E5), Color(0x00000000)),
    )
}

/** Sombra de texto sutil para títulos sobre gradientes. */
val HeroTextShadow = Shadow(color = Color(0x40000000), offset = Offset(0f, 1f), blurRadius = 8f)

private val LightColors = lightColorScheme(
    primary = AppColors.Secondary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E4FB),
    onPrimaryContainer = AppColors.SecondaryDark,
    secondary = AppColors.Primary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E7F4),
    onSecondaryContainer = AppColors.Primary,
    tertiary = AppColors.Accent,
    onTertiary = AppColors.Primary,
    background = AppColors.Background,
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
    onPrimaryContainer = Color(0xFFE5E4FB),
    secondary = AppColors.SecondaryLight,
    onSecondary = AppColors.DarkBackground,
    secondaryContainer = AppColors.DarkSurfaceAlt,
    onSecondaryContainer = Color(0xFFE5E4FB),
    tertiary = AppColors.Accent,
    onTertiary = AppColors.DarkBackground,
    background = AppColors.DarkBackground,
    onBackground = Color(0xFFF1F1FA),
    surface = AppColors.DarkSurface,
    onSurface = Color(0xFFF1F1FA),
    surfaceVariant = AppColors.DarkSurfaceAlt,
    onSurfaceVariant = Color(0xFF9C99B8),
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
        displayLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
        displayMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp, lineHeight = 38.sp, letterSpacing = (-0.25).sp),
        displaySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
        headlineLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.25).sp),
        headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
        titleSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
        bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.15.sp),
        labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp),
    )
}

/** Escala de esquinas — más redondeada y suave que antes (lenguaje "pill / squircle"). */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
