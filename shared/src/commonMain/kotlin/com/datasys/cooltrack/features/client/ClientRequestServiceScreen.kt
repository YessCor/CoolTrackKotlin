package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.ui.components.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla para solicitar un nuevo servicio (Cliente).
 */
class ClientRequestServiceScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        val user = authRepository.state.collectAsState().value.user
        
        var serviceType by remember { mutableStateOf("Mantenimiento") }
        var description by remember { mutableStateOf("") }
        var address by remember { mutableStateOf(user?.address ?: "") }
        var isSubmitting by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
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
                Text("Tipo de Servicio", style = MaterialTheme.typography.titleSmall)
                // Simplificado a un campo de texto para este módulo
                AppInput(
                    value = serviceType,
                    onValueChange = { serviceType = it },
                    placeholder = "Ej. Mantenimiento, Reparación"
                )

                Text("Descripción del problema", style = MaterialTheme.typography.titleSmall)
                AppInput(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Describe brevemente qué necesitas...",
                    minLines = 3
                )

                Text("Dirección de atención", style = MaterialTheme.typography.titleSmall)
                AppInput(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = "Calle, Número, Ciudad"
                )

                Spacer(modifier = Modifier.weight(1f))

                AppButton(
                    text = if (isSubmitting) "Enviando..." else "Enviar Solicitud",
                    onClick = {
                        if (description.isBlank() || address.isBlank()) {
                            scope.launch { toastState.showError("Por favor completa todos los campos") }
                            return@AppButton
                        }
                        
                        scope.launch {
                            isSubmitting = true
                            try {
                                clientRepository.createServiceRequest(
                                    clientId = user?.id ?: "",
                                    equipmentId = null,
                                    serviceType = serviceType,
                                    description = description,
                                    address = address
                                )
                                toastState.showSuccess("Solicitud enviada correctamente")
                                navigator.pop()
                            } catch (e: Exception) {
                                toastState.showError("Error al enviar: ${e.message}")
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting
                )
            }
        }
    }
}
