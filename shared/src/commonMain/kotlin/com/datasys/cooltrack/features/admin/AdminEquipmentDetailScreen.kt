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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.EquipmentType
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppConfirmDialog
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.koinInject

/** Equivalente a admin_equipment_detail_screen.dart. */
@OptIn(ExperimentalMaterial3Api::class)
class AdminEquipmentDetailScreen(private val equipmentId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var equipment by remember { mutableStateOf<Equipment?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isEditing by remember { mutableStateOf(false) }
        var isSaving by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showMenu by remember { mutableStateOf(false) }

        var name by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var serial by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(EquipmentType.SPLIT) }
        var nameError by remember { mutableStateOf<String?>(null) }

        suspend fun load() {
            isLoading = true
            val loaded = adminRepository.getEquipmentById(equipmentId)
            if (loaded != null) {
                equipment = loaded
                name = loaded.name
                brand = loaded.brand ?: ""
                model = loaded.model ?: ""
                serial = loaded.serialNumber ?: ""
                capacity = loaded.capacityTons?.toString() ?: ""
                location = loaded.locationDescription ?: ""
                notes = loaded.notes ?: ""
                selectedType = loaded.type
            }
            isLoading = false
        }

        LaunchedEffect(equipmentId) { load() }

        fun save() {
            nameError = if (name.trim().isEmpty()) "Requerido" else null
            if (nameError != null) return

            scope.launch {
                isSaving = true
                try {
                    val fields = buildMap<String, JsonElement> {
                        put("name", JsonPrimitive(name.trim()))
                        put("type", JsonPrimitive(selectedType.value))
                        brand.trim().takeIf { it.isNotEmpty() }?.let { put("brand", JsonPrimitive(it)) }
                        model.trim().takeIf { it.isNotEmpty() }?.let { put("model", JsonPrimitive(it)) }
                        serial.trim().takeIf { it.isNotEmpty() }?.let { put("serial_number", JsonPrimitive(it)) }
                        capacity.trim().toDoubleOrNull()?.let { put("capacity_tons", JsonPrimitive(it)) }
                        location.trim().takeIf { it.isNotEmpty() }?.let { put("location_description", JsonPrimitive(it)) }
                        notes.trim().takeIf { it.isNotEmpty() }?.let { put("notes", JsonPrimitive(it)) }
                    }
                    adminRepository.updateEquipment(equipmentId, fields)
                    toastState.showSuccess("Equipo actualizado")
                    isEditing = false
                    load()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isSaving = false
                }
            }
        }

        fun delete() {
            scope.launch {
                try {
                    adminRepository.deleteEquipment(equipmentId)
                    toastState.showSuccess("Equipo eliminado")
                    navigator.pop()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del Equipo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.Primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(
                                imageVector = if (isEditing) AppIcons.Close else AppIcons.Edit,
                                contentDescription = if (isEditing) "Cancelar" else "Editar",
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = AppIcons.More, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = AppColors.Error) },
                                    onClick = { showMenu = false; showDeleteDialog = true },
                                )
                            }
                        }
                    },
                )
            },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            val current = equipment
            when {
                isLoading && current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                current == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Equipo no encontrado")
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.Secondary.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(AppColors.Secondary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = AppIcons.Equipment,
                                contentDescription = null,
                                tint = AppColors.Secondary,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(current.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(current.typeLabel, color = AppColors.TextMuted, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Información del Equipo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    AppInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre *",
                        prefixIcon = AppIcons.Equipment,
                        enabled = isEditing,
                        errorText = nameError,
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedType.label,
                            onValueChange = {},
                            readOnly = true,
                            enabled = isEditing,
                            label = { Text("Tipo") },
                            trailingIcon = {
                                if (isEditing) {
                                    Icon(
                                        imageVector = AppIcons.ChevronRight,
                                        contentDescription = "Expandir"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { typeExpanded = !typeExpanded }
                            )
                        }
                        DropdownMenu(
                            expanded = typeExpanded && isEditing,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            EquipmentType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = { selectedType = type; typeExpanded = false },
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            AppInput(value = brand, onValueChange = { brand = it }, label = "Marca", enabled = isEditing)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AppInput(value = model, onValueChange = { model = it }, label = "Modelo", enabled = isEditing)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    AppInput(value = serial, onValueChange = { serial = it }, label = "Número de Serie", enabled = isEditing)
                    Spacer(modifier = Modifier.height(16.dp))
                    AppInput(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = "Capacidad (toneladas)",
                        keyboardType = KeyboardType.Number,
                        enabled = isEditing,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Ubicación", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    AppInput(value = location, onValueChange = { location = it }, label = "Ubicación", maxLines = 2, enabled = isEditing)

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Notas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    AppInput(value = notes, onValueChange = { notes = it }, label = "Notas", maxLines = 3, enabled = isEditing)

                    Spacer(modifier = Modifier.height(24.dp))
                    MaintenanceInfo(current)

                    if (isEditing) {
                        Spacer(modifier = Modifier.height(24.dp))
                        AppButton(label = "Guardar Cambios", onPressed = ::save, isLoading = isSaving, isFullWidth = true)
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AppConfirmDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = ::delete,
                title = "Eliminar Equipo",
                message = "¿Está seguro de eliminar este equipo?",
                confirmText = "Eliminar",
                confirmColor = AppColors.Error,
            )
        }
    }
}

@Composable
private fun MaintenanceInfo(equipment: Equipment) {
    AppCard {
        Text("Información de Mantenimiento", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        if (equipment.installationDate != null) {
            InfoRow("Fecha de Instalación", formatDate(equipment.installationDate))
        }
        if (equipment.lastServiceDate != null) {
            InfoRow("Último Servicio", formatDate(equipment.lastServiceDate))
        }
        if (equipment.installationDate == null && equipment.lastServiceDate == null) {
            Text("Sin información de mantenimiento")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = AppColors.TextMuted)
        Text(value)
    }
}

/** Equivalente a `_formatDate` (`'${date.day}/${date.month}/${date.year}'`). */
private fun formatDate(date: kotlinx.datetime.LocalDate?): String {
    if (date == null) return ""
    return "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
}
