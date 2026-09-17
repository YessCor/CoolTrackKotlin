package com.datasys.cooltrack.features.admin

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Navega de vuelta al dashboard de admin desde cualquier pantalla del stack:
 * si el dashboard ya está en el back stack, hace pop hasta él; si no
 * (p. ej. se navegó desde una pestaña distinta a Dashboard), lo reemplaza.
 */
fun goToAdminDashboard(navigator: Navigator) {
    if (navigator.items.any { it is AdminDashboardScreen }) {
        navigator.popUntil { it is AdminDashboardScreen }
    } else {
        navigator.replaceAll(AdminDashboardScreen())
    }
}

/** Botón uniforme de "volver al dashboard" para el `navigationIcon` de [AppTopBar]. */
@Composable
fun AdminDashboardNavigationIcon(navigator: Navigator) {
    IconButton(onClick = { goToAdminDashboard(navigator) }) {
        Icon(imageVector = AppIcons.Dashboard, contentDescription = "Volver al dashboard")
    }
}
