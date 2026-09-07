package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors

/**
 * Tarjeta base del rediseño: fondo blanco, esquinas de 22dp, borde hairline
 * y una sombra teñida de índigo (en vez del gris plano de Material) que le da
 * profundidad sin ensuciar. `onTap` usa [pressable] (hunde con resorte).
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onTap: (() -> Unit)? = null,
    color: Color? = null,
    elevation: Dp = 10.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    content: @Composable () -> Unit,
) {
    // Por defecto la tarjeta ocupa todo el ancho disponible; un `modifier`
    // con ancho propio lo sigue sobreescribiendo porque va después.
    var m = Modifier
        .fillMaxWidth()
        .then(modifier)
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = AppColors.ShadowTint,
            spotColor = AppColors.ShadowTint,
        )
        .clip(shape)
        .background(color ?: AppColors.Surface)
        .border(1.dp, AppColors.SurfaceBorder, shape)
    if (onTap != null) m = m.pressable(onClick = onTap)
    Column(modifier = m.padding(padding)) { content() }
}

/** Equivalente a AppCardSkeleton en components/card.dart (placeholder de carga). */
@Composable
fun AppCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    var boxModifier = modifier.height(height).background(color = AppColors.SurfaceVariant, shape = shape)
    boxModifier = if (width != null) boxModifier.width(width) else boxModifier.fillMaxWidth()
    Box(modifier = boxModifier)
}

/** Equivalente a AppListTile en components/card.dart. */
@Composable
fun AppListTile(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    val rowModifier = if (onTap != null) modifier.clickable { onTap() } else modifier
    Row(
        modifier = rowModifier.padding(padding).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}
