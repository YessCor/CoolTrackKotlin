package com.datasys.cooltrack.features.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.ui.components.AppIcons

/**
 * Placeholder temporal de `features/notifications/views/notifications_screen.dart`.
 * Se implementa completo en el módulo 5e (Notificaciones); queda acá solo
 * para que la campanita de notificaciones de los dashboards (admin/técnico/
 * cliente) tenga a dónde navegar mientras tanto.
 */
class NotificationsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notificaciones") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Listado de notificaciones — módulo 5e.")
            }
        }
    }
}
