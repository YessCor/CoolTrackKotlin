package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datasys.cooltrack.core.AppColors

/** Equivalente a AppListItem en components/list_item.dart. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    trailingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    showDivider: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = onTap != null || onLongPress != null,
                    onClick = { onTap?.invoke() },
                    onLongClick = { onLongPress?.invoke() },
                )
                .padding(padding),
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
                    Text(text = subtitle, fontSize = 14.sp, color = AppColors.TextMuted)
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = AppColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            trailing?.invoke()
            if (onTap != null) {
                Icon(imageVector = AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
            }
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = AppColors.SurfaceBorder,
            )
        }
    }
}

/** Equivalente a AppSectionHeader en components/list_item.dart. */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    padding: PaddingValues = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(padding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.TextMuted,
            letterSpacing = 0.5.sp,
        )
        trailing?.invoke()
    }
}

/** Equivalente a AppEmptyState en components/list_item.dart. */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextMuted,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = AppColors.TextMuted, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}
