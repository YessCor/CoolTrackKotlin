package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.datasys.cooltrack.models.Client
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.koinInject

/**
 * Equivalente a admin_equipment_new_screen.dart. El `DropdownButtonFormField`
 * de Flutter se reemplaza con `ExposedDropdownMenuBox` de Material3 (el
 * selector estándar de Compose). `clientsListProvider` del original
 * devolvía `List<dynamic>` crudo; acá se decodifica directo a `List<Client>`
 * (más seguro, mismo resultado).
 */
class AdminEquipmentNewScreen(private val clientId: String? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var clients by remember { mutableStateOf<List<Client>?>(null) }
        var selectedClientId by remember { mutableStateOf(clientId) }
        var selectedType by remember { mutableStateOf(EquipmentType.SPLIT) }

        var name by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var serial by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        var nameError by remember { mutableStateOf<String?>(null) }
        var clientError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            clients = try {
                adminRepository.getAllClients()
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun save() {
            nameError = if (name.trim().isEmpty()) "Requerido" else null
            clientError = if (selectedClientId == null) "Requerido" else null
            if (nameError != null || clientError != null) return

            scope.launch {
                isSaving = true
                try {
                    val fields = buildMap<String, JsonElement> {
                        put("client_id", JsonPrimitive(selectedClientId))
                        put("name", JsonPrimitive(name.trim()))
                        put("type", JsonPrimitive(selectedType.value))
                        brand.trim().takeIf { it.isNotEmpty() }?.let { put("brand", JsonPrimitive(it)) }
                        model.trim().takeIf { it.isNotEmpty() }?.let { put("model", JsonPrimitive(it)) }
                        serial.trim().takeIf { it.isNotEmpty() }?.let { put("serial_number", JsonPrimitive(it)) }
                        capacity.trim().toDoubleOrNull()?.let { put("capacity_tons", JsonPrimitive(it)) }
                        location.trim().takeIf { it.isNotEmpty() }?.let { put("location_description", JsonPrimitive(it)) }
                        notes.trim().takeIf { it.isNotEmpty() }?.let { put("notes", JsonPrimitive(it)) }
                    }
                    adminRepository.createEquipment(fields)
                    toastState.showSuccess("Equipo creado")
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
                TopAppBar(
                    title = { Text("Nuevo Equipo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.Primary,
                        titleContentColor = Color.White,
                    ),
                )
            },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Cliente", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                val clientsList = clients
                if (clientsList == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    val selectedName = clientsList.firstOrNull { it.id == selectedClientId }?.name ?: ""
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Seleccionar Cliente *") },
                            isError = clientError != null,
                            supportingText = clientError?.let { { Text(it) } },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            clientsList.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedClientId = c.id
                                        clientError = null
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                Text("Información del Equipo", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre del Equipo *",
                    prefixIcon = AppIcons.Equipment,
                    errorText = nameError,
                )

                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = selectedType.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Equipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        EquipmentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = { selectedType = type; typeExpanded = false },
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        AppInput(value = brand, onValueChange = { brand = it }, label = "Marca")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        AppInput(value = model, onValueChange = { model = it }, label = "Modelo")
                    }
                }

                AppInput(value = serial, onValueChange = { serial = it }, label = "Número de Serie")
                AppInput(value = capacity, onValueChange = { capacity = it }, label = "Capacidad (toneladas)", keyboardType = KeyboardType.Number)

                Text("Ubicación", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                AppInput(value = location, onValueChange = { location = it }, label = "Ubicación", maxLines = 2)

                Text("Notas", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                AppInput(value = notes, onValueChange = { notes = it }, label = "Notas", maxLines = 3)

                Spacer(modifier = Modifier.height(16.dp))
                AppButton(label = "Crear Equipo", onPressed = ::save, isLoading = isSaving, isFullWidth = true)
            }
        }
    }
}
