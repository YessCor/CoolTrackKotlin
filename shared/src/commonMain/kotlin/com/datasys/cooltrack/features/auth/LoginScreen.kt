  package com.datasys.cooltrack.features.auth

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.features.admin.AdminShellScreen
import com.datasys.cooltrack.features.client.ClientShellScreen
import com.datasys.cooltrack.features.tech.TechnicianShellScreen
import com.datasys.cooltrack.ui.components.AppTopBar
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
            containerColor = AppColors.SurfaceVariant,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .verticalScroll(rememberScrollState()),
            ) {
                // --- Header de marca: gradiente frío + ícono en badge circular ---
                // El fondo arranca en y=0 (detrás de la barra de estado) y el
                // padding de status bar se aplica solo al contenido interno,
                // para que el degradado se vea continuo edge-to-edge.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(AppColors.PrimaryDark, AppColors.Primary),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(top = 24.dp, bottom = 40.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(AppColors.Secondary, AppColors.Accent),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AcUnit,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "CoolTrack",
                        style = MaterialTheme.typography.displaySmall,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sistema de mantenimiento HVAC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.75f),
                    )
                }

                // --- Tarjeta de formulario, superpuesta sobre el fondo neutro ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.SurfaceVariant, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .padding(24.dp),
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Inicia sesión",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppColors.TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ingresa tus credenciales para continuar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.TextSecondary,
                    )
                    Spacer(Modifier.height(28.dp))

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
                                Text(
                                    if (obscurePassword) "Ver" else "Ocultar",
                                    color = AppColors.Secondary,
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(4.dp))

                    TextButton(
                        onClick = { navigator.push(ForgotPasswordScreen()) },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("¿Olvidaste tu contraseña?", color = AppColors.Secondary) }

                    Spacer(Modifier.height(12.dp))

                    AppButton(
                        label = if (authState.isLoading) "Ingresando..." else "Iniciar sesión",
                        isLoading = authState.isLoading,
                        isFullWidth = true,
                        height = 54.dp,
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
                AppTopBar(
                    expandedHeight = 44.dp,
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
