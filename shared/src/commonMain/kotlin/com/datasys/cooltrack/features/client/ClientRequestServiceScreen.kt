package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.*
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla para solicitar un nuevo servicio (Cliente).
 */
class ClientRequestServiceScreen : Screen {
    @OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        val user = authRepository.state.collectAsStateSimple().value.user

        val serviceTypes = remember { listOf("Mantenimiento Preventivo", "Mantenimiento Correctivo", "Instalación Nuevo Equipo", "Inspección Técnica") }
        var selectedServiceType by remember { mutableStateOf(serviceTypes.first()) }
        var description by remember { mutableStateOf("") }
        var address by remember { mutableStateOf(user?.address ?: "") }
        var isSubmitting by remember { mutableStateOf(false) }

        var equipmentList by remember { mutableStateOf(listOf<com.datasys.cooltrack.models.Equipment>()) }
        var selectedEquipmentId by remember { mutableStateOf<String?>(null) }
        var showEquipmentDropdown by remember { mutableStateOf(false) }

        LaunchedEffect(user?.id) {
            user?.id?.let { id ->
                try {
                    equipmentList = clientRepository.getMyEquipment(id)
                } catch (e: Exception) {
                    // fallar silencioso, el dropdown mostrará vacío o general
                }
            }
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text("Solicitar Servicio") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            },
            snackbarHost = { AppToastHost(toastState) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Tipo de Servicio Requerido", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                
                // Chips de selección de servicio
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    serviceTypes.forEach { type ->
                        FilterChip(
                            selected = (selectedServiceType == type),
                            onClick = { selectedServiceType = type },
                            label = { Text(type) },
                            leadingIcon = if (selectedServiceType == type) {
                                { Icon(AppIcons.CheckFilled, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Text("Equipo Relacionado (Opcional)", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                AppCard {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            val selectedName = equipmentList.find { it.id == selectedEquipmentId }?.let { "${it.brand} ${it.model} (${it.serialNumber})" }
                                ?: "Sin equipo específico / General"
                            Text(selectedName, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { showEquipmentDropdown = !showEquipmentDropdown }) {
                                Icon(AppIcons.ChevronRight, contentDescription = "Seleccionar equipo")
                            }
                        }
                        DropdownMenu(
                            expanded = showEquipmentDropdown,
                            onDismissRequest = { showEquipmentDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sin equipo específico") },
                                onClick = {
                                    selectedEquipmentId = null
                                    showEquipmentDropdown = false
                                }
                            )
                            equipmentList.forEach { eq ->
                                DropdownMenuItem(
                                    text = { Text("${eq.brand} ${eq.model} - S/N: ${eq.serialNumber}") },
                                    onClick = {
                                        selectedEquipmentId = eq.id
                                        showEquipmentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Descripción del problema", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                AppInput(
                    value = description,
                    onValueChange = { description = it },
                    hint = "Describe detalladamente lo que el equipo presenta o el mantenimiento requerido...",
                    maxLines = 4
                )

                Text("Dirección de Atención", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                AppInput(
                    value = address,
                    onValueChange = { address = it },
                    hint = "Calle, Número, Edificio / Local, Ciudad"
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppButton(
                    label = if (isSubmitting) "Enviando..." else "Enviar Solicitud de Servicio",
                    icon = AppIcons.CheckFilled,
                    onPressed = {
                        if (description.isBlank() || address.isBlank()) {
                            scope.launch { toastState.showError("Por favor completa la descripción y dirección") }
                            return@AppButton
                        }
                        
                        scope.launch {
                            isSubmitting = true
                            try {
                                clientRepository.createServiceRequest(
                                    clientId = user?.id ?: "",
                                    equipmentId = selectedEquipmentId,
                                    serviceType = selectedServiceType,
                                    description = description,
                                    address = address
                                )
                                toastState.showSuccess("¡Solicitud enviada exitosamente!")
                                navigator.pop()
                            } catch (e: Exception) {
                                toastState.showError("Error al enviar solicitud: ${e.message}")
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    isLoading = isSubmitting,
                    isFullWidth = true
                )
            }
        }
    }
}
