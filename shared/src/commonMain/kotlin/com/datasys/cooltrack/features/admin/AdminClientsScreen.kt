package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Client
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppEmptyState
import com.datasys.cooltrack.ui.components.AppIcons
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_clients_screen.dart (incluye su `clientsProvider`
 * local, migrado acá directo como una llamada suspendida en
 * `LaunchedEffect` en vez de un `FutureProvider` aparte).
 */
class AdminClientsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val adminRepository: AdminRepository = koinInject()

        var clients by remember { mutableStateOf<List<Client>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            isLoading = true
            errorMessage = null
            try {
                clients = adminRepository.getAllClients()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
            } finally {
                isLoading = false
            }
        }

        LaunchedEffect(Unit) { load() }

        Scaffold(
            topBar = { AppTopBar(
                    expandedHeight = 44.dp,title = { Text("Clientes") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { navigator.push(AdminClientNewScreen()) }) {
                    Icon(imageVector = AppIcons.Add, contentDescription = "Nuevo cliente")
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when {
                    isLoading && clients == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    errorMessage != null && clients == null -> Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(imageVector = AppIcons.Error, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Error: $errorMessage")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { scope.launch { load() } }) { Text("Reintentar") }
                    }
                    clients?.isEmpty() == true -> AppEmptyState(
                        icon = AppIcons.Clients,
                        title = "No hay clientes",
                        action = {
                            Button(onClick = { navigator.push(AdminClientNewScreen()) }) { Text("Agregar Cliente") }
                        },
                    )
                    clients != null -> LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(clients!!) { client ->
                            AppCard(onTap = { navigator.push(AdminClientDetailScreen(client.id)) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppColors.Secondary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = client.name.take(1).uppercase(),
                                            color = AppColors.Secondary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(client.name, fontWeight = FontWeight.SemiBold)
                                        Text(client.email, color = AppColors.TextMuted, fontSize = 13.sp)
                                    }
                                    if (!client.isActive) {
                                        Box(
                                            modifier = Modifier
                                                .background(AppColors.Error.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text("Inactivo", color = AppColors.Error, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Icon(imageVector = AppIcons.ChevronRight, contentDescription = null, tint = AppColors.TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
