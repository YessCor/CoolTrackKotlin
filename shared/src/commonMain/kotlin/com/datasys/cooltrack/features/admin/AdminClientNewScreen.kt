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
 * Alta de cliente para admin.
 */
class AdminClientNewScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()
        val adminRepository: AdminRepository = koinInject()

        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var emailError by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        fun validate(): Boolean {
            nameError = if (name.trim().isEmpty()) "Ingrese el nombre" else null
            emailError = when {
                email.trim().isEmpty() -> "Ingrese el correo"
                !email.contains("@") -> "Ingrese un correo válido"
                else -> null
            }
            return nameError == null && emailError == null
        }

        fun submit() {
            if (!validate()) return
            scope.launch {
                isLoading = true
                try {
                    adminRepository.createClient(
                        name = name.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                    )
                    toastState.showSuccess("Cliente creado exitosamente")
                    navigator.pop()
                } catch (e: Exception) {
                    toastState.showError("Error: ${e.message}")
                } finally {
                    isLoading = false
                }
            }
        }

        AppFormScaffold(
            title = "Nuevo Cliente",
            subtitle = "Datos de contacto",
            onBack = { navigator.pop() },
            primaryLabel = "Crear Cliente",
            onPrimary = ::submit,
            primaryLoading = isLoading,
            snackbarHost = { AppToastHost(toastState) },
        ) {
            AppFormSection(title = "Información del cliente") {
                AppInput(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre *",
                    prefixIcon = AppIcons.Profile,
                    errorText = nameError,
                )
                AppInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico *",
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
                AppInput(
                    value = address,
                    onValueChange = { address = it },
                    label = "Dirección",
                    prefixIcon = AppIcons.Location,
                    maxLines = 2,
                )
            }
            AppInfoBanner("El cliente podrá iniciar sesión con este correo una vez que tenga una cuenta creada.")
        }
    }
}
