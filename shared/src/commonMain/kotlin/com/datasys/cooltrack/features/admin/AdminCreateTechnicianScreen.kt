package com.datasys.cooltrack.features.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.ui.components.AppFormScaffold
import com.datasys.cooltrack.ui.components.AppFormSection
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInfoBanner
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
 * no puede vivir en el cliente Android por seguridad. Acá solo se crea el
 * perfil en `public.users`; el técnico necesita una cuenta de Auth real
 * creada aparte antes de poder iniciar sesión con ese correo.
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

        AppFormScaffold(
            title = "Crear Técnico",
            subtitle = "Perfil del técnico",
            onBack = { navigator.pop() },
            primaryLabel = "Crear Técnico",
            onPrimary = ::submit,
            primaryLoading = isSaving,
            snackbarHost = { AppToastHost(toastState) },
        ) {
                AppFormSection(title = "Información del técnico") {
                    AppInput(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre completo *",
                        prefixIcon = AppIcons.Profile,
                        errorText = nameError,
                    )
                    AppInput(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email *",
                        prefixIcon = AppIcons.Email,
                        keyboardType = KeyboardType.Email,
                        errorText = emailError,
                    )
                    AppInput(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Teléfono",
                        prefixIcon = AppIcons.Phone,
                        keyboardType = KeyboardType.Phone,
                    )
                }

                AppInfoBanner(
                    "Esto crea el perfil del técnico. Para que pueda iniciar sesión todavía hace falta " +
                        "darle de alta una cuenta con este mismo correo (panel de Supabase o una función " +
                        "server-side).",
                )
        }
    }
}
