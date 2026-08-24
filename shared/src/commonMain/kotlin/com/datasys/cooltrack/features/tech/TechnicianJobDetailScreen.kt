package com.datasys.cooltrack.features.tech

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.datasys.cooltrack.ui.components.AppButtonVariant
import kotlinx.datetime.Clock
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.core.technicianNextStatus
import com.datasys.cooltrack.features.admin.AdminRepository
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Detalle del trabajo para el técnico (Módulo 5c).
 * Permite cambiar estados, agregar notas, evidencia fotográfica y firma del cliente.
 */
data class TechnicianJobDetailScreen(val orderId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val techRepository: TechRepository = koinInject()
        val adminRepository: AdminRepository = koinInject()
        val syncService: com.datasys.cooltrack.services.SyncService = koinInject()
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        var order by remember { mutableStateOf<ServiceOrder?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var notes by remember { mutableStateOf("") }
        var isSavingNotes by remember { mutableStateOf(false) }

        LaunchedEffect(orderId) {
            order = adminRepository.getOrderDetail(orderId)
            order?.let { notes = it.technicianNotes ?: "" }
            isLoading = false
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle de Orden") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                )
            },
            snackbarHost = { AppToastHost(toastState) }
        ) { padding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                order?.let { currentOrder ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header con orden y estado
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Orden #${currentOrder.orderNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    AppStatusBadge(currentOrder.status, large = true)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Servicio: ${currentOrder.serviceType}", style = MaterialTheme.typography.bodyMedium)
                                if (currentOrder.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Descripción: ${currentOrder.description}", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Ubicación
                        Text("Ubicación", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Location, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(currentOrder.address)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Notas del técnico
                        Text("Notas del trabajo", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        AppCard {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Agrega notas sobre el trabajo realizado...") },
                                    minLines = 3,
                                    maxLines = 6,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                AppButton(
                                    label = if (isSavingNotes) "Guardando..." else "Guardar notas",
                                    onPressed = {
                                        scope.launch {
                                            isSavingNotes = true
                                            try {
                                                techRepository.saveTechnicianNotes(currentOrder.id, notes)
                                                toastState.showSuccess("Notas guardadas")
                                            } catch (e: Exception) {
                                                toastState.showError("Error: ${e.message}")
                                            } finally {
                                                isSavingNotes = false
                                            }
                                        }
                                    },
                                    isLoading = isSavingNotes,
                                    isFullWidth = true,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Evidencia fotográfica
                        Text("Evidencia fotográfica", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        var showPhotoPickerModal by remember { mutableStateOf(false) }
                        var photosUploadedCount by remember { mutableIntStateOf(0) }
                        
                        AppCard {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.SurfaceVariant)
                                        .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            AppIcons.Camera,
                                            contentDescription = null,
                                            tint = AppColors.Secondary,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            if (photosUploadedCount > 0) "$photosUploadedCount foto(s) capturada(s)" else "Tomar evidencia de trabajo",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AppButton(
                                    label = "Adjuntar Foto / Evidencia",
                                    icon = AppIcons.Camera,
                                    onPressed = { showPhotoPickerModal = true },
                                    variant = AppButtonVariant.OUTLINE,
                                    isFullWidth = true,
                                )
                            }
                        }

                        if (showPhotoPickerModal) {
                            com.datasys.cooltrack.ui.components.AppConfirmDialog(
                                title = "Subir Evidencia Fotográfica",
                                message = "Selecciona o toma una fotografía del equipo o trabajo realizado.",
                                confirmText = "Simular Captura",
                                cancelText = "Cancelar",
                                onConfirm = {
                                    scope.launch {
                                        try {
                                            val dummyBytes = "CoolTrackEvidenceImageMockData".encodeToByteArray()
                                            syncService.queueMediaUpload(
                                                fileBytes = dummyBytes,
                                                fileName = "evidence_${Clock.System.now().toEpochMilliseconds()}.jpg",
                                                orderId = currentOrder.id,
                                                equipmentId = currentOrder.equipmentId,
                                                context = "evidence",
                                                caption = "Evidencia de trabajo realizada por técnico"
                                            )
                                            photosUploadedCount++
                                            toastState.showSuccess("Evidencia guardada y lista para sincronizar")
                                        } catch (e: Exception) {
                                            toastState.showError("Error al guardar evidencia: ${e.message}")
                                        } finally {
                                            showPhotoPickerModal = false
                                        }
                                    }
                                },
                                onDismissRequest = { showPhotoPickerModal = false }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Firma del cliente
                        Text("Firma del cliente", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        var showSignatureModal by remember { mutableStateOf(false) }
                        var hasSignatureSaved by remember { mutableStateOf(!currentOrder.clientSignatureUrl.isNullOrBlank()) }

                        AppCard {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (hasSignatureSaved) AppColors.Success.copy(alpha = 0.1f) else AppColors.SurfaceVariant)
                                        .border(1.dp, if (hasSignatureSaved) AppColors.Success else AppColors.SurfaceBorder, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            AppIcons.Signature,
                                            contentDescription = null,
                                            tint = if (hasSignatureSaved) AppColors.Success else AppColors.TextMuted,
                                            modifier = Modifier.size(32.dp),
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            if (hasSignatureSaved) "Firma Digital Registrada ✓" else "Solicitar firma de conformidad del cliente",
                                            fontWeight = FontWeight.Medium,
                                            color = if (hasSignatureSaved) AppColors.Success else AppColors.TextPrimary,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                AppButton(
                                    label = if (hasSignatureSaved) "Volver a Firmar" else "Capturar Firma Digital",
                                    icon = AppIcons.Signature,
                                    onPressed = { showSignatureModal = true },
                                    variant = if (hasSignatureSaved) AppButtonVariant.OUTLINE else AppButtonVariant.PRIMARY,
                                    isFullWidth = true,
                                )
                            }
                        }

                        if (showSignatureModal) {
                            com.datasys.cooltrack.ui.components.AppModal(
                                title = "Firma de Conformidad del Cliente",
                                onDismissRequest = { showSignatureModal = false },
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "Solicite al cliente que firme dentro del recuadro:",
                                        fontSize = 13.sp,
                                        color = AppColors.TextSecondary,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    com.datasys.cooltrack.ui.components.SignaturePad(
                                        onSave = { bytes ->
                                            scope.launch {
                                                try {
                                                    syncService.queueSignatureUpload(
                                                        orderId = currentOrder.id,
                                                        fileBytes = bytes,
                                                        fileName = "signature_${currentOrder.id}.png"
                                                    )
                                                    syncService.syncAll()
                                                    hasSignatureSaved = true
                                                    toastState.showSuccess("Firma guardada correctamente")
                                                } catch (e: Exception) {
                                                    toastState.showError("Error al guardar firma: ${e.message}")
                                                } finally {
                                                    showSignatureModal = false
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón de cambio de estado
                        val nextStatus = technicianNextStatus[currentOrder.status]
                        if (nextStatus != null) {
                            AppButton(
                                label = buttonLabelFor(nextStatus),
                                onPressed = {
                                    scope.launch {
                                        try {
                                            techRepository.updateJobStatus(currentOrder.id, nextStatus)
                                            order = adminRepository.getOrderDetail(orderId)
                                            toastState.showSuccess("Estado actualizado a ${nextStatus.label}")
                                        } catch (e: Exception) {
                                            toastState.showError("Error al actualizar: ${e.message}")
                                        }
                                    }
                                },
                                isFullWidth = true,
                            )
                        } else if (currentOrder.status == OrderStatus.COMPLETED) {
                            AppCard {
                                Text(
                                    "Trabajo Finalizado",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    color = AppColors.Success,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buttonLabelFor(status: OrderStatus): String = when (status) {
        OrderStatus.ACCEPTED -> "Aceptar Trabajo"
        OrderStatus.IN_TRANSIT -> "Iniciar Viaje"
        OrderStatus.IN_PROGRESS -> "Iniciar Trabajo"
        OrderStatus.COMPLETED -> "Finalizar Trabajo"
        else -> status.label
    }
}
