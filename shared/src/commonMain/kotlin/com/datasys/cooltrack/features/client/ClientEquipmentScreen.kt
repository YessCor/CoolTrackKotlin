package com.datasys.cooltrack.features.client

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
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.*
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla de lista de equipos para el cliente.
 */
class ClientEquipmentScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()
        
        val user = authRepository.state.collectAsStateSimple().value.user
        var equipment by remember { mutableStateOf<List<Equipment>?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        fun loadData() {
            user?.id?.let { id ->
                scope.launch {
                    isLoading = true
                    equipment = clientRepository.getMyEquipment(id)
                    isLoading = false
                }
            }
        }

        LaunchedEffect(user?.id) {
            loadData()
        }

        Scaffold(
            topBar = { 
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text("Mis Equipos") }
                ) 
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(ClientEquipmentNewScreen()) },
                    containerColor = AppColors.Secondary
                ) {
                    Icon(AppIcons.Add, contentDescription = "Nuevo Equipo")
                }
            }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val list = equipment ?: emptyList()
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Text("Aún no tienes equipos registrados")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list) { eq ->
                            AppCard(
                                onTap = { navigator.push(ClientEquipmentNewScreen(eq)) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        AppIcons.Equipment,
                                        contentDescription = null,
                                        tint = AppColors.Secondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(eq.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${eq.brand} ${eq.model ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                        Text("S/N: ${eq.serialNumber ?: "N/A"}", fontSize = 12.sp, color = AppColors.TextMuted)
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
}
