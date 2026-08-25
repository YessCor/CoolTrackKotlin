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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.Client
import com.datasys.cooltrack.models.Equipment
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_client_detail_screen.dart. `_loadClient()` cargaba el
 * cliente y llenaba los `TextEditingController`; acá el mismo `LaunchedEffect`
 * llena directamente el estado local (`name`, `email`, etc.) que alimenta a
 * `AppInput`.
 */
class AdminClientDetailScreen(private val clientId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var client by remember { mutableStateOf<Client?>(null) }
        var equipment by remember { mutableStateOf<List<Equipment>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isEditing by remember { mutableStateOf(false) }
        var isSaving by remember { mutableStateOf(false) }

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<String?>(null) }

        suspend fun loadClient() {
            isLoading = true
            val loaded = adminRepository.getClientById(clientId)
            if (loaded != null) {
                client = loaded
                name = loaded.name
                email = loaded.email
                phone = loaded.phone ?: ""
                address = loaded.address ?: ""
            }
            isLoading = false
        }

        suspend fun loadEquipment() {
            equipment = try {
                adminRepository.getAllEquipment(clientId)
            } catch (e: Exception) {
                emptyList()
            }
        }

        LaunchedEffect(clientId) {
            loadClient()
            loadEquipment()
        }

        fun save() {
            nameError = if (name.trim().isEmpty()) "Requerido" else null
            if (nameError != null) return

            scope.launch {
                isSaving = true
                try {
                    adminRepository.updateClient(
                        id = clientId,
                        name = name.trim(),
                        email = email.trim().ifEmpty { null },
                        phone = phone.trim().ifEmpty { null },
                        address = address.trim().ifEmpty { null },
                    )
                    toastState.showSuccess("Cliente actualizado")
                    isEditing = false
                    loadClient()
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
                    title = { Text("Detalle del Cliente") },
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
                                contentDescription = if (isEditing) "Cancelar edición" else "Editar",
                            )
                        }
                    },
                )
            },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            if (isLoading && client == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            val current = client
            if (current == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Cliente no encontrado")
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // Header con gradiente (equivalente a LinearGradient(primary, secondary))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(AppColors.Primary, AppColors.Secondary)),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = current.name.take(1).uppercase(),
                            fontSize = 32.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(current.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(current.email, color = Color.White.copy(alpha = 0.8f))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Información del Cliente", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre *",
                    prefixIcon = AppIcons.Profile,
                    enabled = isEditing,
                    errorText = nameError,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    prefixIcon = AppIcons.Email,
                    keyboardType = KeyboardType.Email,
                    enabled = isEditing,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Teléfono",
                    prefixIcon = AppIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                    enabled = isEditing,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppInput(
                    value = address,
                    onValueChange = { address = it },
                    label = "Dirección",
                    prefixIcon = AppIcons.Location,
                    maxLines = 2,
                    enabled = isEditing,
                )

                if (isEditing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        label = "Guardar Cambios",
                        onPressed = ::save,
                        isLoading = isSaving,
                        isFullWidth = true,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Equipos", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { navigator.push(AdminEquipmentNewScreen(clientId = clientId)) },
                    ) { Text("+ Agregar") }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val equipmentList = equipment
                when {
                    equipmentList == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    equipmentList.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.SurfaceVariant, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("No hay equipos registrados") }
                    else -> Column {
                        equipmentList.forEach { eq ->
                            AppCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                onTap = { navigator.push(AdminEquipmentDetailScreen(eq.id)) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppColors.Secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(imageVector = AppIcons.Equipment, contentDescription = null, tint = AppColors.Secondary)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(eq.name, fontWeight = FontWeight.SemiBold)
                                        Text(eq.typeLabel, color = AppColors.TextMuted, fontSize = 13.sp)
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
