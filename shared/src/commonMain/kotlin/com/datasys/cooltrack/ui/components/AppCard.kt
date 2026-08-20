package com.datasys.cooltrack.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datasys.cooltrack.core.AppColors

/**
 * Equivalente a AppCard en components/card.dart. `elevation` acepta `null`
 * para dejar que Material3 use su valor por defecto, igual que el
 * `Card(elevation: elevation)` original.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    onTap: (() -> Unit)? = null,
    color: Color? = null,
    elevation: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit,
) {
    val clickableModifier = if (onTap != null) modifier.clickable { onTap() } else modifier
    Card(
        modifier = clickableModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = color ?: AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(modifier = Modifier.padding(padding)) { content() }
    }
}

/** Equivalente a AppCardSkeleton en components/card.dart (placeholder de carga). */
@Composable
fun AppCardSkeleton(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 80.dp,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    var boxModifier = modifier.height(height).background(color = Color(0xFFE0E0E0), shape = shape)
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
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, fontSize = 14.sp, color = AppColors.TextSecondary)
            }
        }
        if (trailing != null) trailing()
    }
}
