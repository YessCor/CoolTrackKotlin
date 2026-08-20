package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.services.SyncService
import com.datasys.cooltrack.services.SyncStatus
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a SyncIndicator en components/sync_indicator.dart.
 *
 * El original suscribía un listener manual (`_syncService.addListener`) en
 * `initState`/`dispose`; acá simplemente se observa el `StateFlow<SyncStatus>`
 * que ya expone `SyncService` (mismo patrón que el resto de la UI) con
 * `collectAsStateSimple()`. `getPendingCount()` no es reactivo en el
 * original tampoco (se relee a mano en cada cambio de estado), así que acá
 * se recalcula de la misma forma cada vez que cambia `status`.
 */
@Composable
fun SyncIndicator(modifier: Modifier = Modifier) {
    val syncService: SyncService = koinInject()
    val status by syncService.status.collectAsStateSimple()
    val scope = rememberCoroutineScope()

    var pendingCount by remember { mutableIntStateOf(syncService.getPendingCount()) }
    LaunchedEffect(status) {
        pendingCount = syncService.getPendingCount()
    }

    if (pendingCount == 0 && status != SyncStatus.SYNCING) {
        return
    }

    val isSyncing = status == SyncStatus.SYNCING
    val isError = status == SyncStatus.ERROR
    val color = if (isError) AppColors.Error else AppColors.Secondary

    Row(
        modifier = modifier
            .clickable(enabled = !isSyncing) { scope.launch { syncService.syncAll() } }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = AppColors.Secondary,
            )
        } else {
            Icon(
                imageVector = if (isError) AppIcons.SyncProblem else AppIcons.Sync,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isSyncing) "Sincronizando..." else "$pendingCount pendientes",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}
