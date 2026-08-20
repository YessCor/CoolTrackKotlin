package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.ApiClient
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppButtonVariant
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Equivalente a admin_client_new_screen.dart. El `Form` + `GlobalKey<FormState>`
 * de Flutter se reemplaza con validación manual en `submit()`, ya que
 * `AppInput` (módulo 5a) no tiene un `FormFieldValidator` acoplado — mismo
 * criterio que el resto de las pantallas de este módulo.
 */
class AdminClientNewScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val toastState = rememberAppToastState()

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
                    ApiClient.post(
                        "/clients",
                        buildJsonObject {
                            put("name", name.trim())
                            put("email", email.trim())
                            put("phone", phone.trim())
                            put("address", address.trim())
                        },
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

        Scaffold(
            topBar = { TopAppBar(title = { Text("Nuevo Cliente") }) },
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
                Spacer(modifier = Modifier.height(16.dp))
                AppButton(
                    label = "Crear Cliente",
                    onPressed = ::submit,
                    isLoading = isLoading,
                    isFullWidth = true,
                    variant = AppButtonVariant.PRIMARY,
                )
            }
        }
    }
}
