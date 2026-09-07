package com.datasys.cooltrack.core

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Estado global de marca configurable en tiempo de ejecución (pantalla de
 * Ajustes). `accent` y `logo` viven como `mutableStateOf`, así que cambiarlos
 * recompone toda la app: `AppColors`/`AppGradients` leen de acá.
 *
 * El valor se persiste con [AppSettingsStore] y se restaura en el arranque.
 */
object AppBranding {
    /** Índigo por defecto. */
    val DefaultAccent = Color(0xFF4F46E5)

    var accent by mutableStateOf(DefaultAccent)
        internal set

    var logo by mutableStateOf(AppLogoMark.SNOWFLAKE)
        internal set

    /** Presets de color ofrecidos en Ajustes (nombre visible -> color de acción). */
    val accentPresets: List<Pair<String, Color>> = listOf(
        "Índigo" to Color(0xFF4F46E5),
        "Azure" to Color(0xFF2563EB),
        "Cielo" to Color(0xFF0EA5E9),
        "Cian" to Color(0xFF0891B2),
        "Violeta" to Color(0xFF7C3AED),
        "Esmeralda" to Color(0xFF059669),
        "Rosa" to Color(0xFFDB2777),
        "Naranja" to Color(0xFFEA580C),
        "Grafito" to Color(0xFF475569),
    )
}

/** Marcas de logo disponibles (todas dibujadas con Compose, ver AppLogo.kt). */
enum class AppLogoMark { SNOWFLAKE, FAN, DROPLET, HEXAGON, BOLT }

/**
 * Paleta "Frost" de CoolTrack. El color de acción (`Secondary`/`Accent`/…)
 * deriva de [AppBranding.accent] y es configurable; el resto (navy de los
 * headers, superficies, texto, estados semánticos) es fijo.
 *
 * Se mantienen los mismos nombres de campo de siempre para no romper
 * referencias — solo que ahora varios son `get()` reactivos.
 */
object AppColors {
    private val acc: Color get() = AppBranding.accent

    // Marca oscura — navy con un toque del acento, para headers heroicos y modo oscuro.
    val Primary: Color get() = lerp(Color(0xFF1E1B4B), acc, 0.12f)
    val PrimaryLight: Color get() = lerp(Color(0xFF312E81), acc, 0.15f)
    val PrimaryDark: Color get() = Color(0xFF14122E)

    // Color de acción principal (configurable).
    val Secondary: Color get() = acc
    val SecondaryLight: Color get() = lerp(acc, Color.White, 0.20f)
    val SecondaryDark: Color get() = lerp(acc, Color.Black, 0.16f)

    // Acento secundario para gradientes (una versión más luminosa/fría del acento).
    val Accent: Color get() = lerp(acc, Color(0xFF22D3EE), 0.55f)

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFF43F5E)
    val Info: Color get() = acc

    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF4F4FB)
    val SurfaceBorder = Color(0xFFE7E7F4)
    val Outline = Color(0xFF9C99B8)

    val Background = Color(0xFFF6F6FC)

    val TextPrimary = Color(0xFF1B1930)
    val TextSecondary = Color(0xFF56526E)
    val TextMuted = Color(0xFF908CA8)

    /** Sombra teñida del acento — da "peso" a las tarjetas sin verse gris sucio. */
    val ShadowTint: Color get() = acc.copy(alpha = 0.10f)

    // Dark mode
    val DarkBackground = Color(0xFF0B0A1A)
    val DarkSurface = Color(0xFF16142B)
    val DarkSurfaceAlt = Color(0xFF201D3D)
    val DarkBorder = Color(0xFF322E52)

    val StatusPending = Color(0xFFF59E0B)
    val StatusAssigned: Color get() = acc
    val StatusAccepted = Color(0xFF7C3AED)
    val StatusInTransit = Color(0xFF06B6D4)
    val StatusInProgress: Color get() = lerp(acc, Color.White, 0.18f)
    val StatusCompleted = Color(0xFF10B981)
    val StatusCancelled = Color(0xFFF43F5E)

    val QuoteDraft = Color(0xFF7C7A99)
    val QuoteSent: Color get() = acc
    val QuoteApproved = Color(0xFF10B981)
    val QuoteRejected = Color(0xFFF43F5E)
    val QuoteExpired = Color(0xFFF59E0B)

    fun forOrderStatus(status: OrderStatus): Color = when (status) {
        OrderStatus.PENDING -> StatusPending
        OrderStatus.ASSIGNED -> StatusAssigned
        OrderStatus.ACCEPTED -> StatusAccepted
        OrderStatus.IN_TRANSIT -> StatusInTransit
        OrderStatus.IN_PROGRESS -> StatusInProgress
        OrderStatus.COMPLETED -> StatusCompleted
        OrderStatus.CANCELLED -> StatusCancelled
    }

    fun forQuoteStatus(status: QuoteStatus): Color = when (status) {
        QuoteStatus.DRAFT -> QuoteDraft
        QuoteStatus.SENT -> QuoteSent
        QuoteStatus.APPROVED -> QuoteApproved
        QuoteStatus.REJECTED -> QuoteRejected
        QuoteStatus.EXPIRED -> QuoteExpired
    }
}

/**
 * Gradientes de marca, derivados del acento. El "hero" (navy -> acento ->
 * acento luminoso) es la firma visual: aparece en headers, splash y login.
 */
object AppGradients {
    val heroColors: List<Color>
        get() = listOf(AppColors.PrimaryDark, AppColors.Secondary, AppColors.Accent)
    val heroSoftColors: List<Color>
        get() = listOf(AppColors.PrimaryLight, AppColors.Secondary)
    val accentColors: List<Color>
        get() = listOf(AppColors.SecondaryLight, AppColors.Accent)

    fun hero(): Brush = Brush.linearGradient(heroColors)
    fun heroVertical(): Brush = Brush.verticalGradient(heroColors)
    fun heroSoft(): Brush = Brush.linearGradient(heroSoftColors)
    fun accent(): Brush = Brush.linearGradient(accentColors)

    fun halo(): Brush = Brush.radialGradient(
        colors = listOf(AppColors.Secondary.copy(alpha = 0.18f), Color(0x00000000)),
    )
}

/** Sombra de texto sutil para títulos sobre gradientes. */
val HeroTextShadow = Shadow(color = Color(0x40000000), offset = Offset(0f, 1f), blurRadius = 8f)

private fun lightColors() = lightColorScheme(
    primary = AppColors.Secondary,
    onPrimary = Color.White,
    primaryContainer = lerp(AppColors.Secondary, Color.White, 0.86f),
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

private fun darkColors() = darkColorScheme(
    primary = AppColors.SecondaryLight,
    onPrimary = AppColors.DarkBackground,
    primaryContainer = AppColors.SecondaryDark,
    onPrimaryContainer = lerp(AppColors.Secondary, Color.White, 0.8f),
    secondary = AppColors.SecondaryLight,
    onSecondary = AppColors.DarkBackground,
    secondaryContainer = AppColors.DarkSurfaceAlt,
    onSecondaryContainer = lerp(AppColors.Secondary, Color.White, 0.8f),
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

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun CooltrackTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    // Se lee `AppBranding.accent` acá (dentro de la composición) para que un
    // cambio de color en Ajustes recomponga todo el árbol con el nuevo esquema.
    val scheme = if (darkTheme) darkColors() else lightColors()
    MaterialTheme(
        colorScheme = scheme,
        typography = appTypography(),
        shapes = AppShapes,
        content = content,
    )
}
