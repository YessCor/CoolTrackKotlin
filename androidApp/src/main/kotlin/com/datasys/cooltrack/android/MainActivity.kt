package com.datasys.cooltrack.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppSettingsStore
import com.datasys.cooltrack.core.CooltrackTheme
import com.datasys.cooltrack.navigation.CooltrackApp
import org.koin.android.ext.android.inject

/**
 * Monta el árbol de Compose y dispara la carga inicial (branding + sesión).
 *
 * En release aplica FLAG_SECURE: bloquea capturas de pantalla, grabación de
 * pantalla y la vista previa en "apps recientes" — la app maneja datos de
 * clientes y no debería quedar en capturas ni en grabaciones.
 */
class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        enableEdgeToEdge()

        setContent {
            LaunchedEffect(Unit) {
                AppSettingsStore.load()
                authRepository.init()
            }

            CooltrackTheme {
                CooltrackApp(authRepository)
            }
        }
    }
}
