package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datasys.cooltrack.core.AppColors

/** Equivalente a AppButtonVariant en components/button.dart. */
enum class AppButtonVariant { PRIMARY, SECONDARY, OUTLINE, TEXT }

/**
 * Equivalente a AppButton en components/button.dart.
 *
 * Los cuatro `case` de `_getButtonStyle()`/`_getTextStyle()` del original
 * se resuelven acá con `ButtonDefaults.buttonColors(...)` /
 * `OutlinedButton`/`TextButton` según `variant`, manteniendo los mismos
 * colores, radios (12dp) y paddings (24/12dp) que el original.
 */
@Composable
fun AppButton(
    label: String,
    onPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    isLoading: Boolean = false,
    isFullWidth: Boolean = false,
    icon: ImageVector? = null,
    width: Dp? = null,
    height: Dp = 48.dp,
) {
    val shape = RoundedCornerShape(12.dp)
    val contentPadding = ButtonDefaults.ContentPadding

    var sizedModifier = modifier.height(height)
    sizedModifier = when {
        isFullWidth -> sizedModifier.fillMaxWidth()
        width != null -> sizedModifier.width(width)
        else -> sizedModifier.wrapContentWidth()
    }

    val spinnerColor = if (variant == AppButtonVariant.OUTLINE || variant == AppButtonVariant.TEXT) {
        AppColors.Secondary
    } else {
        Color.White
    }

    val content: @Composable () -> Unit = {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = spinnerColor,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    fontSize = if (variant == AppButtonVariant.TEXT) 14.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    when (variant) {
        AppButtonVariant.PRIMARY -> Button(
            onClick = { onPressed?.invoke() },
            enabled = !isLoading && onPressed != null,
            modifier = sizedModifier,
            shape = shape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Secondary,
                contentColor = Color.White,
                disabledContainerColor = AppColors.Secondary.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
            ),
        ) { content() }

        AppButtonVariant.SECONDARY -> Button(
            onClick = { onPressed?.invoke() },
            enabled = !isLoading && onPressed != null,
            modifier = sizedModifier,
            shape = shape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.Primary,
                contentColor = Color.White,
                disabledContainerColor = AppColors.Primary.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
            ),
        ) { content() }

        AppButtonVariant.OUTLINE -> OutlinedButton(
            onClick = { onPressed?.invoke() },
            enabled = !isLoading && onPressed != null,
            modifier = sizedModifier,
            shape = shape,
            contentPadding = contentPadding,
            border = BorderStroke(1.dp, AppColors.Secondary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Secondary),
        ) { content() }

        AppButtonVariant.TEXT -> TextButton(
            onClick = { onPressed?.invoke() },
            enabled = !isLoading && onPressed != null,
            modifier = sizedModifier,
            shape = shape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Secondary),
        ) { content() }
    }
}

/**
 * Equivalente a AppIconButton en components/button.dart. `tooltip` se
 * ignora en la implementación común (Compose Multiplatform no tiene un
 * `Tooltip` unificado entre Android/iOS todavía); queda como parámetro
 * documental por si se agrega soporte más adelante.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    onPressed: (() -> Unit)?,
    modifier: Modifier = Modifier,
    color: Color? = null,
    size: Dp = 24.dp,
    tooltip: String? = null,
) {
    IconButton(onClick = { onPressed?.invoke() }, modifier = modifier, enabled = onPressed != null) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            modifier = Modifier.size(size),
            tint = color ?: AppColors.TextMuted,
        )
    }
}
