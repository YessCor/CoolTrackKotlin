package com.datasys.cooltrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.CooltrackTheme
import com.datasys.cooltrack.navigation.CooltrackApp
import org.koin.android.ext.android.inject

/**
 * Equivalente al `runApp(ProviderScope(child: MyApp()))` + `MaterialApp.router`
 * de lib/main.dart. `ProviderScope` (contexto global de Riverpod) es acá
 * `startKoin` (en CooltrackApplication); este Activity solo monta el árbol
 * de Compose y dispara `authRepository.init()` — equivalente al
 * `AuthNotifier()` que se auto-inicializaba al crear el provider.
 */
class MainActivity : ComponentActivity() {

    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaunchedEffect(Unit) {
                authRepository.init()
            }

            CooltrackTheme {
                CooltrackApp(authRepository)
            }
        }
    }
}
