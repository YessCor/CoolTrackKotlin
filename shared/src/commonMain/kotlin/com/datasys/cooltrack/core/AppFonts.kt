package com.datasys.cooltrack.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Familia tipográfica de marca (Plus Jakarta Sans). Cada plataforma provee
 * su propio mecanismo de carga de fuentes (Android: `res/font` + `Font()`;
 * iOS: system font como fallback, ya que empaquetar TTF en el framework
 * requiere un paso de build aparte fuera del alcance de este cambio).
 */
@Composable
expect fun appFontFamily(): FontFamily
