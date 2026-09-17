package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.datasys.cooltrack.models.User
import com.datasys.cooltrack.ui.components.AppTopBar
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Detalle/edición de técnico. [AdminRepository] no tiene `getTechnicianById`,
 * así que se busca dentro de [AdminRepository.getAllTechnicians] por id, igual
 * que hacían las pantallas de lista original antes de tener detalle propio.
 */
class AdminTechnicianDetailScreen(private val technicianId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var technician by remember { mutableStateOf<User?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var isEditing by remember { mutableStateOf(false) }
        var isSaving by remember { mutableStateOf(false) }
        var isDeleting by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var passwordError by remember { mutableStateOf<String?>(null) }

        suspend fun loadTechnician() {
            isLoading = true
            val loaded = adminRepository.getAllTechnicians().find { it.id == technicianId }
            if (loaded != null) {
                technician = loaded
                name = loaded.name
                email = loaded.email
                phone = loaded.phone ?: ""
                password = ""
                confirmPassword = ""
            }
            isLoading = false
        }

        LaunchedEffect(technicianId) { loadTechnician() }

        fun save() {
            nameError = if (name.trim().isEmpty()) "Requerido" else null
            passwordError = when {
                password.isEmpty() && confirmPassword.isEmpty() -> null
                password.length < 6 -> "Mínimo 6 caracteres"
                password != confirmPassword -> "Las contraseñas no coinciden"
                else -> null
            }
            if (nameError != null || passwordError != null) return

            scope.launch {
                isSaving = true
                try {
                    adminRepository.updateTechnicianWithAuth(
                        id = technicianId,
                        name = name.trim(),
                        email = email.trim().ifEmpty { null },
                        password = password.ifEmpty { null },
                        phone = phone.trim().ifEmpty { null },
                    )
                    toastState.showSuccess("Técnico actualizado")
                    isEditing = false
                    loadTechnician()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isSaving = false
                }
            }
        }

        fun delete() {
            scope.launch {
                isDeleting = true
                try {
                    adminRepository.deleteUser(technicianId)
                    toastState.showSuccess("Técnico eliminado")
                    navigator.pop()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isDeleting = false
                    showDeleteConfirm = false
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
                title = { Text("Eliminar técnico") },
                text = { Text("¿Seguro que deseas eliminar a ${technician?.name ?: "este técnico"}? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = ::delete, enabled = !isDeleting) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }, enabled = !isDeleting) { Text("Cancelar") }
                },
            )
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = { Text("Detalle del Técnico") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.Primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                    navigationIcon = { AdminDashboardNavigationIcon(navigator) },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(imageVector = AppIcons.Delete, contentDescription = "Eliminar técnico")
                        }
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
            if (isLoading && technician == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }
            val current = technician
            if (current == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Técnico no encontrado")
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
                Text("Información del Técnico", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

                if (isEditing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AppInput(
                        value = password,
                        onValueChange = { password = it },
                        label = "Nueva contraseña (opcional)",
                        prefixIcon = AppIcons.Lock,
                        obscureText = true,
                        enabled = isEditing,
                        errorText = passwordError,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppInput(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirmar contraseña",
                        prefixIcon = AppIcons.Lock,
                        obscureText = true,
                        enabled = isEditing,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        label = "Guardar Cambios",
                        onPressed = ::save,
                        isLoading = isSaving,
                        isFullWidth = true,
                    )
                }
            }
        }
    }
}
