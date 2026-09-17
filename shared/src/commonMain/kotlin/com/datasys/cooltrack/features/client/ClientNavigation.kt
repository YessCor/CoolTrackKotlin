package com.datasys.cooltrack.features.client

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Navega de vuelta al inicio del cliente ("Mis Servicios") desde cualquier
 * pantalla del stack: si ya está en el back stack hace pop hasta él, si no
 * (p. ej. se navegó desde otra pestaña), lo reemplaza. Mismo patrón que
 * [com.datasys.cooltrack.features.admin.goToAdminDashboard] y
 * [com.datasys.cooltrack.features.tech.goToTechnicianDashboard].
 */
fun goToClientHome(navigator: Navigator) {
    if (navigator.items.any { it is ClientOrdersScreen }) {
        navigator.popUntil { it is ClientOrdersScreen }
    } else {
        navigator.replaceAll(ClientOrdersScreen())
    }
}

/** Botón uniforme de "volver al inicio" para el `navigationIcon` de [AppTopBar]. */
@Composable
fun ClientHomeNavigationIcon(navigator: Navigator) {
    IconButton(onClick = { goToClientHome(navigator) }) {
        Icon(imageVector = AppIcons.Home, contentDescription = "Volver al inicio")
    }
}
