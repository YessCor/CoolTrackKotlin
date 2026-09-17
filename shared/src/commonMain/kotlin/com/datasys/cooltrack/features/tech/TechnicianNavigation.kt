package com.datasys.cooltrack.features.tech

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Navega de vuelta al dashboard de técnico desde cualquier pantalla del stack:
 * si el dashboard ya está en el back stack, hace pop hasta él; si no (p. ej.
 * se navegó desde otra pestaña), lo reemplaza. Mismo patrón que
 * [com.datasys.cooltrack.features.admin.goToAdminDashboard].
 */
fun goToTechnicianDashboard(navigator: Navigator) {
    if (navigator.items.any { it is TechnicianDashboardScreen }) {
        navigator.popUntil { it is TechnicianDashboardScreen }
    } else {
        navigator.replaceAll(TechnicianDashboardScreen())
    }
}

/** Botón uniforme de "volver al inicio" para el `navigationIcon` de [AppTopBar]. */
@Composable
fun TechnicianHomeNavigationIcon(navigator: Navigator) {
    IconButton(onClick = { goToTechnicianDashboard(navigator) }) {
        Icon(imageVector = AppIcons.Home, contentDescription = "Volver al inicio")
    }
}
