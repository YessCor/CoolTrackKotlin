package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.QuoteStatus

private fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING -> Color(0xFFFF9800) // Colors.orange
    OrderStatus.ASSIGNED -> Color(0xFF2196F3) // Colors.blue
    OrderStatus.ACCEPTED -> Color(0xFF00BCD4) // Colors.cyan
    OrderStatus.IN_TRANSIT -> Color(0xFF9C27B0) // Colors.purple
    OrderStatus.IN_PROGRESS -> Color(0xFF3F51B5) // Colors.indigo
    OrderStatus.COMPLETED -> Color(0xFF4CAF50) // Colors.green
    OrderStatus.CANCELLED -> Color(0xFFF44336) // Colors.red
}

private fun orderStatusIcon(status: OrderStatus): ImageVector = when (status) {
    OrderStatus.PENDING -> AppIcons.HourglassEmpty
    OrderStatus.ASSIGNED -> AppIcons.ProfileFilled
    OrderStatus.ACCEPTED -> AppIcons.Check
    OrderStatus.IN_TRANSIT -> AppIcons.DirectionsCar
    OrderStatus.IN_PROGRESS -> AppIcons.Build
    OrderStatus.COMPLETED -> AppIcons.CheckFilled
    OrderStatus.CANCELLED -> AppIcons.Cancel
}

/**
 * Equivalente a AppStatusBadge en components/status_badge.dart. La etiqueta
 * ya no necesita un `orderStatusLabels[status] ?? 'Desconocido'` aparte:
 * `OrderStatus.label` (de `core/Constants.kt`) ya trae el texto en español.
 */
@Composable
fun AppStatusBadge(status: OrderStatus, large: Boolean = false, modifier: Modifier = Modifier) {
    val color = orderStatusColor(status)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(if (large) 12.dp else 8.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(if (large) 12.dp else 8.dp))
            .padding(horizontal = if (large) 16.dp else 10.dp, vertical = if (large) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = orderStatusIcon(status),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (large) 18.dp else 14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (large) 14.sp else 12.sp,
        )
    }
}

private fun quoteStatusColor(status: QuoteStatus): Color = when (status) {
    QuoteStatus.DRAFT -> Color(0xFF9E9E9E) // Colors.grey
    QuoteStatus.SENT -> Color(0xFF2196F3) // Colors.blue
    QuoteStatus.APPROVED -> Color(0xFF4CAF50) // Colors.green
    QuoteStatus.REJECTED -> Color(0xFFF44336) // Colors.red
    QuoteStatus.EXPIRED -> Color(0xFFFF9800) // Colors.orange
}

private fun quoteStatusIcon(status: QuoteStatus): ImageVector = when (status) {
    QuoteStatus.DRAFT -> AppIcons.Edit
    QuoteStatus.SENT -> AppIcons.Send
    QuoteStatus.APPROVED -> AppIcons.CheckFilled
    QuoteStatus.REJECTED -> AppIcons.Cancel
    QuoteStatus.EXPIRED -> AppIcons.TimerOff
}

/** Equivalente a AppQuoteStatusBadge en components/status_badge.dart. */
@Composable
fun AppQuoteStatusBadge(status: QuoteStatus, large: Boolean = false, modifier: Modifier = Modifier) {
    val color = quoteStatusColor(status)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(if (large) 12.dp else 8.dp))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.3f)), RoundedCornerShape(if (large) 12.dp else 8.dp))
            .padding(horizontal = if (large) 16.dp else 10.dp, vertical = if (large) 8.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = quoteStatusIcon(status),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (large) 18.dp else 14.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (large) 14.sp else 12.sp,
        )
    }
}
