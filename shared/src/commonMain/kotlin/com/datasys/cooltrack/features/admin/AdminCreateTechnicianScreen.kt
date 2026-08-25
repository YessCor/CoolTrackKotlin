package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a admin_create_technician_screen.dart.
 *
 * Nota importante: crear una cuenta de Supabase Auth para *otra* persona
 * (con contraseña) requiere el `service_role` (admin API de Supabase), que
 * no puede vivir en el cliente Android por seguridad — es el mismo motivo
 * por el que no se usó la `service_role` key para configurar esta app. Acá
 * solo se crea el perfil en `public.users`; el técnico necesita una cuenta
 * de Auth real creada aparte (panel de Supabase, o una Edge Function
 * server-side) antes de poder iniciar sesión con ese correo.
 */
class AdminCreateTechnicianScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        var nameError by remember { mutableStateOf<String?>(null) }
        var emailError by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        fun validate(): Boolean {
            nameError = if (name.trim().isEmpty()) "Requerido" else null
            emailError = when {
                email.trim().isEmpty() -> "Requerido"
                !email.contains("@") -> "Email inválido"
                else -> null
            }
            return listOf(nameError, emailError).all { it == null }
        }

        fun submit() {
            if (!validate()) return

            scope.launch {
                isSaving = true
                try {
                    adminRepository.createTechnicianProfile(
                        name = name.trim(),
                        email = email.trim(),
                        phone = phone.trim().ifEmpty { null },
                    )
                    toastState.showSuccess("Perfil de técnico creado")
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
                    title = { Text("Crear Técnico") },
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
            ) {
                Text("Información del Técnico", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre Completo *",
                    prefixIcon = AppIcons.Profile,
                    errorText = nameError,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email *",
                    prefixIcon = AppIcons.Email,
                    keyboardType = KeyboardType.Email,
                    errorText = emailError,
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppInput(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Teléfono",
                    prefixIcon = AppIcons.Phone,
                    keyboardType = KeyboardType.Phone,
                )

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1A2196F3), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                ) {
                    Icon(imageVector = AppIcons.Info, contentDescription = null, tint = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Esto crea el perfil del técnico. Para que pueda iniciar sesión todavía hace " +
                            "falta darle de alta una cuenta con este mismo correo (panel de Supabase o " +
                            "una función server-side) — un cliente Android no puede crear cuentas de otros " +
                            "usuarios de forma segura.",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                AppButton(label = "Crear Técnico", onPressed = ::submit, isLoading = isSaving, isFullWidth = true)
            }
        }
    }
}
