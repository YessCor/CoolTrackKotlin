package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.models.TechnicianLocation
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppEmptyState
import com.datasys.cooltrack.ui.components.AppIcons
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

/**
 * Equivalente a admin_tech_tracking_screen.dart.
 *
 * El original dibuja un `GoogleMap` (paquete `google_maps_flutter`) con un
 * marcador por técnico, actualizado en vivo desde
 * `technician_locations` de Supabase Realtime. Este proyecto KMP todavía
 * no tiene un SDK de mapas agregado (no hay Google Maps Compose para
 * Android + un puente nativo para iOS en `build.gradle.kts`) — agregar eso
 * es un trabajo de integración aparte, similar en tamaño al de
 * `ImagePickerService`/`PdfRenderer`.
 *
 * En vez de dejar la pantalla como placeholder vacío, esta versión
 * preserva la parte que sí importa (la suscripción en tiempo real a la
 * ubicación de cada técnico) y la muestra como una lista con nombre,
 * coordenadas y hora de la última actualización — mismo dato que mostraba
 * el `InfoWindow` del marcador original, sin el mapa visual. Cambiar esto
 * por un mapa real más adelante no toca la suscripción realtime, solo la
 * capa de presentación.
 */
class AdminTechTrackingScreen : Screen {
    @Composable
    override fun Content() {
        val supabase: SupabaseClient = koinInject()
        val adminRepository: AdminRepository = koinInject()

        var technicianNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var latestLocations by remember { mutableStateOf<Map<String, TechnicianLocation>>(emptyMap()) }

        suspend fun refetchLocations() {
            val rows = supabase.from("technician_locations")
                .select(Columns.ALL) { order("recorded_at", order = io.github.jan.supabase.postgrest.query.Order.ASCENDING) }
                .decodeList<TechnicianLocation>()
            // Igual que `_updateMarkers` en el original: se agrupa por
            // técnico y solo se queda la última ubicación de cada uno
            // (como la consulta viene ordenada ascendente, la última
            // ocurrencia por `technicianId` es la más reciente).
            latestLocations = rows.associateBy { it.technicianId }
        }

        LaunchedEffect(Unit) {
            technicianNames = try {
                adminRepository.getActiveTechnicians().associate { it.id to it.name }
            } catch (e: Exception) {
                emptyMap()
            }

            try {
                refetchLocations()
            } catch (e: Exception) {
                // se sigue igual: la suscripción de abajo puede traer datos después
            }

            val channel = supabase.realtime.channel("technician-locations-admin")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "technician_locations"
            }
            channel.subscribe()
            changeFlow.collect {
                try {
                    refetchLocations()
                } catch (e: Exception) {
                    // ignorar: se reintenta en el próximo evento realtime
                }
            }
        }

        Scaffold(topBar = { TopAppBar(title = { Text("Rastreo de Técnicos") }) }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (latestLocations.isEmpty()) {
                    AppEmptyState(
                        icon = AppIcons.Map,
                        title = "Sin técnicos en línea",
                        message = "En cuanto un técnico comparta su ubicación, va a aparecer acá.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(latestLocations.values.toList()) { loc ->
                            AppCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppColors.Info.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(imageVector = AppIcons.Location, contentDescription = null, tint = AppColors.Info)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            technicianNames[loc.technicianId] ?: "Técnico",
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            "Lat ${formatCoord(loc.latitude)}, Lng ${formatCoord(loc.longitude)}",
                                            fontSize = 12.sp,
                                            color = AppColors.TextMuted,
                                        )
                                        Text(
                                            "Última actualización: ${formatTime(loc.recordedAt)}",
                                            fontSize = 12.sp,
                                            color = AppColors.TextMuted,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCoord(value: Double): String = (kotlin.math.round(value * 10000) / 10000).toString()

private fun formatTime(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth}/${dt.monthNumber} ${dt.hour}:$minute"
}
