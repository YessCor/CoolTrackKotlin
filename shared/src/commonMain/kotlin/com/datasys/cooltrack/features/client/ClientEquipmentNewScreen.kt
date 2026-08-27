package com.datasys.cooltrack.features.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.EquipmentType
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.*
import com.datasys.cooltrack.util.collectAsStateSimple
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Pantalla para registrar o editar un equipo (Cliente).
 */
data class ClientEquipmentNewScreen(val existingEquipment: Equipment? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clientRepository: ClientRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

        val user = authRepository.state.collectAsStateSimple().value.user
        val isEditing = existingEquipment != null

        var name by remember { mutableStateOf(existingEquipment?.name ?: "") }
        var brand by remember { mutableStateOf(existingEquipment?.brand ?: "") }
        var model by remember { mutableStateOf(existingEquipment?.model ?: "") }
        var serial by remember { mutableStateOf(existingEquipment?.serialNumber ?: "") }
        var capacity by remember { mutableStateOf(existingEquipment?.capacityTons?.toString() ?: "") }
        var location by remember { mutableStateOf(existingEquipment?.locationDescription ?: "") }
        var notes by remember { mutableStateOf(existingEquipment?.notes ?: "") }
        var selectedType by remember { mutableStateOf(existingEquipment?.type ?: EquipmentType.SPLIT) }
        
        var isSaving by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        fun save() {
            if (name.isBlank()) {
                scope.launch { toastState.showError("El nombre es requerido") }
                return
            }

            scope.launch {
                isSaving = true
                try {
                    if (isEditing) {
                        clientRepository.updateEquipment(
                            id = existingEquipment!!.id,
                            name = name,
                            type = selectedType.value,
                            brand = brand.ifBlank { null },
                            model = model.ifBlank { null },
                            serialNumber = serial.ifBlank { null },
                            capacityTons = capacity.toDoubleOrNull(),
                            location = location.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                        toastState.showSuccess("Equipo actualizado")
                    } else {
                        clientRepository.createEquipment(
                            clientId = user?.id ?: "",
                            name = name,
                            type = selectedType.value,
                            brand = brand.ifBlank { null },
                            model = model.ifBlank { null },
                            serialNumber = serial.ifBlank { null },
                            capacityTons = capacity.toDoubleOrNull(),
                            location = location.ifBlank { null },
                            notes = notes.ifBlank { null }
                        )
                        toastState.showSuccess("Equipo registrado")
                    }
                    navigator.pop()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isSaving = false
                }
            }
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text(if (isEditing) "Editar Equipo" else "Nuevo Equipo") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    actions = {
                        if (isEditing) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(AppIcons.Delete, contentDescription = "Eliminar", tint = AppColors.Error)
                            }
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
                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del Equipo (ej: Sala, Recámara Principal)*",
                    prefixIcon = AppIcons.Equipment
                )

                Text("Tipo de Unidad", fontWeight = FontWeight.SemiBold)
                var typeExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { typeExpanded = !typeExpanded }) {
                                Icon(AppIcons.ChevronRight, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        EquipmentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = { selectedType = type; typeExpanded = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppInput(value = brand, onValueChange = { brand = it }, label = "Marca")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AppInput(value = model, onValueChange = { model = it }, label = "Modelo")
                    }
                }

                AppInput(value = serial, onValueChange = { serial = it }, label = "Número de Serie")
                
                AppInput(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = "Capacidad (Toneladas)",
                    keyboardType = KeyboardType.Number
                )

                AppInput(value = location, onValueChange = { location = it }, label = "Ubicación / Piso", prefixIcon = AppIcons.Location)

                AppInput(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notas Técnicas (ej: requiere escalera, difícil acceso)",
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                AppButton(
                    label = if (isSaving) "Guardando..." else "Guardar Equipo",
                    onPressed = ::save,
                    isLoading = isSaving,
                    isFullWidth = true
                )
            }
        }

        if (showDeleteDialog) {
            AppConfirmDialog(
                title = "Eliminar Equipo",
                message = "¿Estás seguro que deseas eliminar este equipo? Esta acción no se puede deshacer.",
                confirmText = "Eliminar",
                confirmColor = AppColors.Error,
                onConfirm = {
                    scope.launch {
                        try {
                            clientRepository.deleteEquipment(existingEquipment!!.id)
                            toastState.showSuccess("Equipo eliminado")
                            navigator.pop()
                        } catch (e: Exception) {
                            toastState.showError("Error al eliminar: ${e.message}")
                        }
                    }
                },
                onDismissRequest = { showDeleteDialog = false }
            )
        }
    }
}
