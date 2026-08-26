package com.datasys.cooltrack.features.tech

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppEmptyState
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.SyncIndicator
import org.koin.compose.koinInject

/**
 * Pantalla de listado de trabajos para el técnico (Módulo 5c).
 * Muestra las órdenes asignadas y pendientes de ejecución.
 */
class TechnicianJobsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val techRepository: TechRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        
        val user = authRepository.state.collectAsState().value.user
        var jobs by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(user?.id) {
            user?.id?.let { id ->
                jobs = techRepository.getAssignedJobs(id)
                isLoading = false
            }
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text("Mis Trabajos") },
                    actions = {
                        SyncIndicator()
                    }
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val list = jobs ?: emptyList()
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        AppEmptyState(
                            icon = AppIcons.Build,
                            title = "No tienes trabajos asignados",
                            message = "Cuando el administrador te asigne una orden, aparecerá aquí.",
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list) { job ->
                            JobItem(job) {
                                navigator.push(TechnicianJobDetailScreen(job.id))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun JobItem(job: ServiceOrder, onClick: () -> Unit) {
        AppCard(onTap = onClick) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Orden #${job.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    AppStatusBadge(job.status)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Location,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        job.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Calendar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        job.scheduledDate?.toString() ?: "No programada",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextMuted
                    )
                }
            }
        }
    }
}
