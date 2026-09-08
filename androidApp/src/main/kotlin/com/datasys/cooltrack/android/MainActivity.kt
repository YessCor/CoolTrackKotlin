package com.datasys.cooltrack.android

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppSettingsStore
import com.datasys.cooltrack.core.CooltrackTheme
import com.datasys.cooltrack.navigation.CooltrackApp
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Monta el árbol de Compose y dispara la carga inicial (branding + sesión).
 *
 * Splash: la SplashScreen del sistema se mantiene visible mientras se
 * restaura la sesión guardada y se carga la personalización de marca, así
 * la primera vista aparece ya con su color/logo y sin parpadeo en blanco.
 * Tiene un tope de [MAX_SPLASH_MS] para que nunca se quede colgado.
 *
 * En release aplica FLAG_SECURE: bloquea capturas de pantalla, grabación de
 * pantalla y la vista previa en "apps recientes".
 */
class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        val startedAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            elapsed < MAX_SPLASH_MS && !authRepository.state.value.isInitialized
        }

        // Arranca la carga fuera de Compose para que el splash se libere
        // apenas la sesión esté lista, sin esperar el warm-up del árbol.
        lifecycleScope.launch {
            AppSettingsStore.load()
            authRepository.init()
        }

        if (!BuildConfig.DEBUG) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }

        enableEdgeToEdge()

        setContent {
            CooltrackTheme {
                CooltrackApp(authRepository)
            }
        }
    }

    private companion object {
        const val MAX_SPLASH_MS = 2500L
    }
}
