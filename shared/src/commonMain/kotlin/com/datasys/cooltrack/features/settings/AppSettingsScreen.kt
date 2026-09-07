package com.datasys.cooltrack.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.core.AppBranding
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.AppLogoMark
import com.datasys.cooltrack.core.AppSettingsStore
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppHeroScaffold
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppLogo
import com.datasys.cooltrack.ui.components.AppLogoGlyph
import com.datasys.cooltrack.ui.components.AppSectionTitle
import com.datasys.cooltrack.ui.components.pressable
import kotlinx.coroutines.launch

/**
 * Ajustes de personalización de marca: color de acción de toda la app y
 * logo. Los cambios se aplican al instante (toda la UI se recompone) y se
 * guardan con [AppSettingsStore].
 */
class AppSettingsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        // Lecturas directas: `AppBranding.*` es `mutableStateOf`, así que
        // esta pantalla se recompone sola cuando cambian.
        val accentArgb = AppBranding.accent.toArgb()
        val currentLogo = AppBranding.logo

        AppHeroScaffold(
            title = "Ajustes",
            onBack = { navigator.pop() },
            heroContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppLogo(size = 56.dp, corner = 16.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("CoolTrack", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Personalización", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            },
        ) {
            // --- Color ---
            AppSectionTitle("Color de la app")
            Spacer(Modifier.height(8.dp))
            Text(
                "Se aplica a botones, headers, íconos y acentos en toda la app.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppBranding.accentPresets.chunked(5).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            rowItems.forEach { (name, color) ->
                                SwatchDot(
                                    color = color,
                                    selected = color.toArgb() == accentArgb,
                                    label = name,
                                    modifier = Modifier.weight(1f),
                                ) { scope.launch { AppSettingsStore.saveAccent(color) } }
                            }
                            repeat(5 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // --- Logo ---
            AppSectionTitle("Logo")
            Spacer(Modifier.height(12.dp))
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppLogoMark.entries.forEach { mark ->
                        LogoOption(
                            mark = mark,
                            selected = mark == currentLogo,
                            modifier = Modifier.weight(1f),
                        ) { scope.launch { AppSettingsStore.saveLogo(mark) } }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // --- Preview ---
            AppSectionTitle("Vista previa")
            Spacer(Modifier.height(12.dp))
            AppCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppLogo(size = 64.dp)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("CoolTrack", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Sistema de mantenimiento HVAC", style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.Secondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Botón primario", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchDot(color: Color, selected: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color)
                .pressable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(AppIcons.Check, contentDescription = "Seleccionado", tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextMuted, maxLines = 1)
    }
}

@Composable
private fun LogoOption(mark: AppLogoMark, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AppColors.Secondary.copy(alpha = 0.12f) else AppColors.SurfaceVariant)
            .pressable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        AppLogoGlyph(
            modifier = Modifier.size(26.dp),
            color = if (selected) AppColors.Secondary else AppColors.TextMuted,
            mark = mark,
        )
    }
}
