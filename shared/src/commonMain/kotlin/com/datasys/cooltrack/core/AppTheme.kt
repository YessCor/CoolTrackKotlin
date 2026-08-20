package com.datasys.cooltrack.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Equivalente a AppColors en lib/core/theme.dart */
object AppColors {
    val Primary = Color(0xFF0D1B2A)
    val PrimaryLight = Color(0xFF1B263B)
    val PrimaryDark = Color(0xFF0A1628)

    val Secondary = Color(0xFF00B4D8)
    val SecondaryLight = Color(0xFF48CAE4)
    val SecondaryDark = Color(0xFF0096C7)

    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)

    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)
    val SurfaceBorder = Color(0xFFE2E8F0)
    val Outline = Color(0xFF94A3B8)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)
    val TextMuted = Color(0xFF94A3B8)

    val StatusPending = Color(0xFFF59E0B)
    val StatusAssigned = Color(0xFF3B82F6)
    val StatusAccepted = Color(0xFF8B5CF6)
    val StatusInTransit = Color(0xFF06B6D4)
    val StatusInProgress = Color(0xFF6366F1)
    val StatusCompleted = Color(0xFF22C55E)
    val StatusCancelled = Color(0xFFEF4444)

    /** Mapea OrderStatus -> color, útil para chips de estado en toda la UI. */
    fun forOrderStatus(status: UserFacingOrderStatus): Color = when (status) {
        UserFacingOrderStatus.PENDING -> StatusPending
        UserFacingOrderStatus.ASSIGNED -> StatusAssigned
        UserFacingOrderStatus.ACCEPTED -> StatusAccepted
        UserFacingOrderStatus.IN_TRANSIT -> StatusInTransit
        UserFacingOrderStatus.IN_PROGRESS -> StatusInProgress
        UserFacingOrderStatus.COMPLETED -> StatusCompleted
        UserFacingOrderStatus.CANCELLED -> StatusCancelled
    }
}

/** Alias liviano para no acoplar AppColors al enum OrderStatus del módulo core. */
enum class UserFacingOrderStatus { PENDING, ASSIGNED, ACCEPTED, IN_TRANSIT, IN_PROGRESS, COMPLETED, CANCELLED }

private val LightColors = lightColorScheme(
    primary = AppColors.Primary,
    secondary = AppColors.Secondary,
    surface = AppColors.Surface,
    error = AppColors.Error,
    background = AppColors.Surface,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.SecondaryLight,
    secondary = AppColors.Secondary,
    surface = AppColors.PrimaryDark,
    error = AppColors.Error,
    background = AppColors.PrimaryDark,
)

/** Equivalente a AppTheme.light / AppTheme.dark, aplicado con MaterialTheme (Compose Multiplatform). */
@Composable
fun CooltrackTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
