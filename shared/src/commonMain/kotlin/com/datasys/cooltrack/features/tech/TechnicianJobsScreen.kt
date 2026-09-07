package com.datasys.cooltrack.features.tech

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.*
import org.koin.compose.koinInject

/**
 * Pantalla de listado de trabajos para el técnico.
 */
class TechnicianJobsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val techRepository: TechRepository = koinInject()
        val authRepository: AuthRepository = koinInject()

        val user = authRepository.state.collectAsState().value.user
        var jobs by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                user?.id?.let { jobs = techRepository.getAssignedJobs(it) }
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar tus trabajos"
            }
        }

        LaunchedEffect(user?.id) { load() }

        AppListScreen(
            title = "Mis Trabajos",
            subtitle = jobs?.let { "${it.size} asignados" },
            data = jobs,
            error = error,
            emptyIcon = AppIcons.Build,
            emptyTitle = "No tienes trabajos asignados",
            emptyMessage = "Cuando el administrador te asigne una orden, aparecerá aquí.",
            heroTrailing = { SyncIndicator() },
        ) { list ->
            itemsIndexed(list) { index, job ->
                AppCard(
                    modifier = Modifier.appEnter(index),
                    onTap = { navigator.push(TechnicianJobDetailScreen(job.id)) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLeadingIcon(AppIcons.Build, tint = AppColors.forOrderStatus(job.status))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Orden #${job.orderNumber}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        AppStatusBadge(job.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    IconLine(AppIcons.Location, job.address)
                    Spacer(Modifier.height(4.dp))
                    IconLine(AppIcons.Calendar, formatShortDate(job.scheduledDate?.toString()).takeIf { it != "—" } ?: "No programada")
                }
            }
        }
    }

    @Composable
    private fun IconLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = AppColors.TextMuted)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 1)
        }
    }
}
