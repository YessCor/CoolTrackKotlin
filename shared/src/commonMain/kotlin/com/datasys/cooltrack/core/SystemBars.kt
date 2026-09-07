package com.datasys.cooltrack.core

import androidx.compose.runtime.Composable

/**
 * Ajusta el color de los íconos de la barra de estado según el fondo que
 * tiene debajo la pantalla actual:
 *  - `darkIcons = true`  -> fondo claro (pantallas planas / hoja de contenido)
 *  - `darkIcons = false` -> fondo oscuro (headers hero con gradiente)
 *
 * En iOS es no-op (el status bar se maneja por Info.plist / UIViewController).
 */
@Composable
expect fun StatusBarIcons(darkIcons: Boolean)
