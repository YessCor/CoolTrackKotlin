package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.AppAsyncContent
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLeadingIcon
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.Spacing
import com.datasys.cooltrack.ui.components.staggeredItem
import org.koin.compose.koinInject

/** Equivalente a admin_equipment_screen.dart (con su `equipmentProvider` local). */
class AdminEquipmentScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val adminRepository: AdminRepository = koinInject()
        var equipment by remember { mutableStateOf<List<Equipment>?>(null) }
        var error by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            error = null
            try {
                equipment = adminRepository.getAllEquipment()
            } catch (e: Exception) {
                error = e.message ?: "No se pudieron cargar los equipos"
            }
        }

        LaunchedEffect(Unit) { load() }

        AppScreenScaffold(
            title = "Equipos",
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(AdminEquipmentNewScreen()) },
                    containerColor = AppColors.Secondary,
                    contentColor = Color.White,
                ) {
                    Icon(AppIcons.Add, contentDescription = "Nuevo equipo")
                }
            },
        ) { padding ->
            AppAsyncContent(
                data = equipment,
                error = error,
                emptyIcon = AppIcons.Equipment,
                emptyTitle = "No hay equipos",
                emptyMessage = "Registrá los equipos de tus clientes para llevar su historial.",
                modifier = Modifier.padding(padding),
            ) { list ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = Spacing.screen,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(list) { index, item ->
                        AppCard(
                            modifier = Modifier.staggeredItem(index),
                            onTap = { navigator.push(AdminEquipmentDetailScreen(item.id)) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLeadingIcon(AppIcons.Equipment, tint = AppColors.Secondary)
                                Spacer(Modifier.width(Spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${item.typeLabel} · ${item.brand ?: "Sin marca"}",
                                        color = AppColors.TextMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Icon(AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
