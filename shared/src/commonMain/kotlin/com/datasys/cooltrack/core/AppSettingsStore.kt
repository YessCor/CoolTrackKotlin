package com.datasys.cooltrack.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Persiste la personalización de marca (color de acción + logo) usando el
 * [SecureStorage] que ya trae la app. `load()` se llama una vez en el
 * arranque; `saveAccent`/`saveLogo` desde la pantalla de Ajustes.
 */
object AppSettingsStore {
    private const val KEY_ACCENT = "branding_accent_argb"
    private const val KEY_LOGO = "branding_logo_mark"

    suspend fun load() {
        SecureStorage.read(KEY_ACCENT)?.toIntOrNull()?.let {
            AppBranding.accent = Color(it)
        }
        SecureStorage.read(KEY_LOGO)?.let { name ->
            runCatching { AppLogoMark.valueOf(name) }.getOrNull()?.let { AppBranding.logo = it }
        }
    }

    suspend fun saveAccent(color: Color) {
        AppBranding.accent = color
        SecureStorage.write(KEY_ACCENT, color.toArgb().toString())
    }

    suspend fun saveLogo(mark: AppLogoMark) {
        AppBranding.logo = mark
        SecureStorage.write(KEY_LOGO, mark.name)
    }
}
