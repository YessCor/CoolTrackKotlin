package com.datasys.cooltrack.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.StateFlow

/**
 * Puente reutilizable entre `StateFlow<T>` (usado en todos los repositorios
 * del módulo `shared`) y el `State<T>` de Compose — equivalente a
 * `ref.watch(someProvider)` de Riverpod, pero framework-agnostic. Se usa en
 * todas las pantallas en vez de repetir la implementación en cada una.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateSimple(): State<T> =
    produceState(initialValue = value) {
        collect { value = it }
    }
