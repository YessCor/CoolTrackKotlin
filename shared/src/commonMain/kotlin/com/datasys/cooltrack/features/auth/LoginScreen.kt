package com.datasys.cooltrack.features.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.AppGradients
import com.datasys.cooltrack.features.admin.AdminShellScreen
import com.datasys.cooltrack.features.client.ClientShellScreen
import com.datasys.cooltrack.features.tech.TechnicianShellScreen
import com.datasys.cooltrack.ui.components.AppButton
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppInput
import com.datasys.cooltrack.ui.components.AppScreenScaffold
import com.datasys.cooltrack.ui.components.AppToastHost
import com.datasys.cooltrack.ui.components.appEnter
import com.datasys.cooltrack.ui.components.appPop
import com.datasys.cooltrack.ui.components.rememberAppToastState
import com.datasys.cooltrack.util.collectAsStateSimple
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Login rediseñado: hero a sangre completa con el gradiente de marca y
 * círculos difusos, logo que aparece con "pop", y una hoja de formulario
 * que sube sobre el fondo. Cada campo entra escalonado.
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

        com.datasys.cooltrack.core.StatusBarIcons(darkIcons = false)
        Scaffold(
            containerColor = AppColors.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
            ) {
                // --- Hero de marca ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(AppGradients.heroVertical()),
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(alpha = 0.08f), size.minDimension * 0.55f, Offset(size.width * 0.9f, size.height * 0.15f))
                        drawCircle(AppColors.Accent.copy(alpha = 0.16f), size.minDimension * 0.4f, Offset(size.width * 0.1f, size.height * 0.92f))
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 36.dp, bottom = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .appPop()
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.AcUnit, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "CoolTrack",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            modifier = Modifier.appEnter(index = 1),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Sistema de mantenimiento HVAC",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.appEnter(index = 2),
                        )
                    }
                }

                // --- Hoja de formulario ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                ) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Inicia sesión",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.appEnter(index = 3),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ingresa tus credenciales para continuar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.appEnter(index = 4),
                    )
                    Spacer(Modifier.height(24.dp))

                    AppCard(modifier = Modifier.appEnter(index = 5)) {
                        AppInput(
                            value = email,
                            onValueChange = { email = it },
                            label = "Correo electrónico",
                            hint = "tu@correo.com",
                            prefixIcon = AppIcons.Email,
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            errorText = if (emailError) "El correo es requerido" else null,
                        )
                        Spacer(Modifier.height(16.dp))
                        AppInput(
                            value = password,
                            onValueChange = { password = it },
                            label = "Contraseña",
                            hint = "••••••••",
                            prefixIcon = AppIcons.Lock,
                            obscureText = obscurePassword,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            errorText = if (passwordError) "La contraseña es requerida" else null,
                            suffix = {
                                TextButton(onClick = { obscurePassword = !obscurePassword }) {
                                    Text(if (obscurePassword) "Ver" else "Ocultar", color = AppColors.Secondary)
                                }
                            },
                        )
                    }

                    TextButton(
                        onClick = { navigator.push(ForgotPasswordScreen()) },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("¿Olvidaste tu contraseña?", color = AppColors.Secondary) }

                    Spacer(Modifier.height(8.dp))

                    AppButton(
                        label = if (authState.isLoading) "Ingresando..." else "Iniciar sesión",
                        icon = AppIcons.ArrowForward,
                        isLoading = authState.isLoading,
                        isFullWidth = true,
                        height = 56.dp,
                        modifier = Modifier.appEnter(index = 6),
                        onPressed = {
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
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

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

        AppScreenScaffold(
            title = "Recuperar contraseña",
            onBack = { navigator.pop() },
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
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier.size(88.dp).appPop().clip(CircleShape).background(AppColors.Secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Lock, contentDescription = null, tint = AppColors.Secondary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "¿Olvidaste tu contraseña?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ingresa tu correo y te enviaremos las instrucciones para restablecerla.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))

                AppCard {
                    AppInput(
                        value = email,
                        onValueChange = { email = it },
                        label = "Correo electrónico",
                        prefixIcon = AppIcons.Email,
                        keyboardType = KeyboardType.Email,
                        errorText = if (submitted && email.isBlank()) "El correo es requerido" else null,
                        autofocus = true,
                    )
                }
                Spacer(Modifier.height(20.dp))

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
                    Spacer(Modifier.height(20.dp))
                    AppCard(color = AppColors.Success.copy(alpha = 0.1f)) {
                        Text(successMessage!!, style = MaterialTheme.typography.bodyMedium, color = AppColors.Success)
                    }
                }
            }
        }
    }
}
