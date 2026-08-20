package com.datasys.cooltrack.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.auth.AuthState
import com.datasys.cooltrack.core.UserRole
import com.datasys.cooltrack.features.admin.AdminShellScreen
import com.datasys.cooltrack.features.auth.ForgotPasswordScreen
import com.datasys.cooltrack.features.auth.LoginScreen
import com.datasys.cooltrack.features.client.ClientShellScreen
import com.datasys.cooltrack.features.tech.TechnicianShellScreen
import com.datasys.cooltrack.util.collectAsStateSimple

/**
 * Equivalente a core/router.dart (GoRouter + redirect por rol).
 *
 * go_router declara rutas como strings ("/admin", "/technician", "/client")
 * con un `redirect` global. Acá el mismo "redirect" se resuelve eligiendo
 * qué `Screen` de Voyager mostrar según el rol del AuthState — sin strings
 * de ruta que se puedan escribir mal.
 *
 * Los ShellRoute de go_router (AdminLayout/TechLayout/ClientLayout con su
 * child) son `AdminShellScreen`/`TechnicianShellScreen`/`ClientShellScreen`
 * — cada una envuelve su propio Navigator anidado (tabs internas) en el
 * módulo de features.
 */
@Composable
fun CooltrackApp(authRepository: AuthRepository) {
    val authState: AuthState by authRepository.state.collectAsStateSimple()

    if (!authState.isInitialized) {
        // Splash simple mientras se restaura la sesión (loadStoredAuth)
        return
    }

    Navigator(initialScreenFor(authState)) { navigator ->
        // Redirect reactivo: si el estado de auth cambia (login/logout) en
        // caliente, navegamos igual que hacía el `redirect` de go_router.
        LaunchedEffect(authState.isAuthenticated, authState.role) {
            val current = navigator.lastItem
            val isAuthRoute = current is LoginScreen || current is ForgotPasswordScreen

            if (!authState.isAuthenticated && !isAuthRoute) {
                navigator.replaceAll(LoginScreen())
            } else if (authState.isAuthenticated && isAuthRoute) {
                navigator.replaceAll(initialScreenFor(authState))
            }
        }

        CurrentScreen()
    }
}

private fun initialScreenFor(authState: AuthState): Screen = when {
    !authState.isAuthenticated -> LoginScreen()
    authState.role == UserRole.ADMIN -> AdminShellScreen()
    authState.role == UserRole.TECHNICIAN -> TechnicianShellScreen()
    else -> ClientShellScreen()
}
