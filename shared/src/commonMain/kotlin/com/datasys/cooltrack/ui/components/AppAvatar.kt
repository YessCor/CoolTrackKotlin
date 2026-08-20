package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.datasys.cooltrack.core.AppColors

/**
 * Equivalente a AppAvatar en components/avatar.dart. `Image.network(...,
 * errorBuilder: ...)` se reemplaza por Coil3 `AsyncImage`, que ya hace
 * fallback automático de red/caché; el fallback a iniciales se resuelve
 * mostrándolas siempre debajo de la imagen y dejando que Coil solo dibuje
 * encima cuando la carga es exitosa (mismo resultado visual que el
 * `errorBuilder` original, sin depender de callbacks de error por
 * plataforma).
 */
@Composable
fun AppAvatar(
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    name: String? = null,
    size: Dp = 48.dp,
    onTap: (() -> Unit)? = null,
    showBorder: Boolean = false,
) {
    val clickableModifier = if (onTap != null) modifier.clickable { onTap() } else modifier
    Box(
        modifier = clickableModifier
            .size(size)
            .clip(CircleShape)
            .background(AppColors.Secondary.copy(alpha = 0.1f))
            .then(
                if (showBorder) {
                    Modifier.border(BorderStroke(2.dp, AppColors.Secondary), CircleShape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsFor(name),
            color = AppColors.Secondary,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp,
        )
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private fun initialsFor(name: String?): String {
    if (name.isNullOrEmpty()) return "?"
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase()
    } else {
        name.first().uppercase()
    }
}

/** Equivalente a AppAvatarGroup en components/avatar.dart. */
@Composable
fun AppAvatarGroup(
    imageUrls: List<String?>,
    modifier: Modifier = Modifier,
    maxDisplay: Int = 3,
    size: Dp = 36.dp,
    overlap: Dp = 8.dp,
) {
    val displayCount = if (imageUrls.size > maxDisplay) maxDisplay else imageUrls.size
    val remaining = imageUrls.size - maxDisplay

    Row(modifier = modifier.size(size)) {
        for (index in 0 until displayCount) {
            Box(
                modifier = Modifier
                    .offset(x = (index * -overlap.value).dp)
                    .border(BorderStroke(2.dp, Color.White), CircleShape),
            ) {
                AppAvatar(imageUrl = imageUrls[index], size = size)
            }
        }
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (displayCount * -overlap.value).dp)
                    .size(size)
                    .clip(CircleShape)
                    .background(AppColors.Primary)
                    .border(BorderStroke(2.dp, Color.White), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+$remaining", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
