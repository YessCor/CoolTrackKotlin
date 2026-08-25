  package com.datasys.cooltrack.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.features.admin.AdminShellScreen
import com.datasys.cooltrack.features.client.ClientShellScreen
import com.datasys.cooltrack.features.tech.TechnicianShellScreen
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.rememberAppToastState
import com.datasys.cooltrack.util.collectAsStateSimple
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Equivalente a features/auth/views/login_screen.dart.
 *
 * `ConsumerStatefulWidget` + `ref.watch(authProvider)` de Riverpod se
 * traduce a: Voyager `Screen` + `koinInject<AuthRepository>()` +
 * `collectAsState()` sobre su `StateFlow`. El resto (formulario, validación,
 * snackbar de error) es una traducción directa widget-a-composable.
 */
class LoginScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authRepository: AuthRepository = koinInject()
        val authState by authRepository.state.collectAsStateSimple()
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var obscurePassword by remember { mutableStateOf(true) }
        var submitted by remember { mutableStateOf(false) }

        val emailError = submitted && email.isBlank()
        val passwordError = submitted && password.isBlank()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.height(80.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "CoolTrack",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppColors.Primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "HVAC Maintenance System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
                Spacer(Modifier.height(48.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    isError = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    isError = passwordError,
                    visualTransformation = if (obscurePassword) PasswordVisualTransformation() else VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { obscurePassword = !obscurePassword }) {
                            Text(if (obscurePassword) "Ver" else "Ocultar")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = { navigator.push(ForgotPasswordScreen()) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("¿Olvidaste tu contraseña?") }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        submitted = true
                        if (email.isNotBlank() && password.isNotBlank()) {
                            scope.launch {
                                val success = authRepository.login(email.trim(), password)
                                if (!success) {
                                    val error = authRepository.state.value.error ?: "Error de inicio de sesión"
                                    snackbarHostState.showSnackbar(error)
                                }
                            }
                        }
                    },
                    enabled = !authState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    } else {
                        Text("Iniciar sesión")
                    }
                }
            }
        }

        // Redirect reactivo tras login exitoso — el resto lo maneja
        // CooltrackApp/AppNavigation observando el mismo AuthRepository.
        LaunchedEffect(authState.isAuthenticated) {
            if (authState.isAuthenticated) {
                val target = when {
                    authState.isAdmin -> AdminShellScreen()
                    authState.isTechnician -> TechnicianShellScreen()
                    else -> ClientShellScreen()
                }
                navigator.replaceAll(target)
            }
        }
    }
}

/** Pantalla de recuperación de contraseña. */
class ForgotPasswordScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val toastState = rememberAppToastState()
        val scope = rememberCoroutineScope()
        val supabase: SupabaseClient = koinInject()

        var email by remember { mutableStateOf("") }
        var submitted by remember { mutableStateOf(false) }
        var isLoading by remember { mutableStateOf(false) }
        var successMessage by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recuperar contraseña") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(AppIcons.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
            snackbarHost = { AppToastHost(toastState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(32.dp))
                Icon(
                    imageVector = AppIcons.Lock,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.height(64.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.Primary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ingresa tu correo electrónico y te enviaremos las instrucciones para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
                Spacer(Modifier.height(32.dp))

                AppInput(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    prefixIcon = AppIcons.Email,
                    keyboardType = KeyboardType.Email,
                    errorText = if (submitted && email.isBlank()) "El correo es requerido" else null,
                    autofocus = true,
                )
                Spacer(Modifier.height(24.dp))

                AppButton(
                    label = if (isLoading) "Enviando..." else "Enviar instrucciones",
                    onPressed = {
                        submitted = true
                        if (email.isNotBlank()) {
                            scope.launch {
                                isLoading = true
                                try {
                                    supabase.auth.resetPasswordForEmail(email.trim())
                                    successMessage = "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."
                                    toastState.showSuccess("Correo enviado")
                                } catch (e: Exception) {
                                    toastState.showError("Error: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    isLoading = isLoading,
                    isFullWidth = true,
                )

                if (successMessage != null) {
                    Spacer(Modifier.height(24.dp))
                    AppCard {
                        Text(
                            text = successMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.Success,
                        )
                    }
                }
            }
        }
    }
}
