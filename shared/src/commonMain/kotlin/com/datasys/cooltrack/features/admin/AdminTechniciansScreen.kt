package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.User
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.AppTag
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.staggeredItem
import org.koin.compose.koinInject

/** Equivalente a admin_technicians_screen.dart (con su `techniciansProvider` local, vía REST). */
class AdminTechniciansScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        var technicians by remember { mutableStateOf<List<User>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                technicians = adminRepository.getAllTechnicians()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar los técnicos"
            }
        }

        LaunchedEffect(Unit) { load() }

        AppScreenScaffold(title = "Técnicos") { padding ->
            AppAsyncContent(
                data = technicians,
                error = error,
                emptyIcon = AppIcons.Technicians,
                emptyTitle = "No hay técnicos",
                emptyMessage = "Creá el primer técnico para poder asignarle órdenes.",
                emptyAction = {
                    AppButton(
                        label = "Nuevo técnico",
                        icon = AppIcons.Add,
                        onPressed = { navigator.push(AdminCreateTechnicianScreen()) },
                    )
                },
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, tech ->
                        AppCard(modifier = Modifier.staggeredItem(index)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(AppColors.Secondary.copy(alpha = 0.14f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        tech.name.take(1).uppercase(),
                                        color = AppColors.Secondary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(tech.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(tech.phone ?: tech.email, color = AppColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                AppTag(
                                    text = if (tech.isActive) "Activo" else "Inactivo",
                                    color = if (tech.isActive) AppColors.Success else AppColors.Error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
