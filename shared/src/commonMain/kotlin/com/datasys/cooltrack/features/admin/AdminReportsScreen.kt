package com.datasys.cooltrack.features.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppIcons
import org.koin.compose.koinInject

/**
 * Equivalente a admin_reports_screen.dart. El original usaba `fl_chart`
 * (`PieChart`/`BarChart`), una librería específica de Flutter sin
 * equivalente 1:1 en Compose Multiplatform; acá los gráficos se dibujan a
 * mano con `Canvas` (mismo criterio ya usado para `SignaturePad` en el
 * módulo 5a). Diferencia menor: las etiquetas de cada porción del pie
 * (`title` dibujado encima de la porción en el original) se movieron a una
 * leyenda al costado — más legible en porciones chicas y más fácil de
 * verificar sin una librería de medición de texto sobre arcos.
 */
class AdminReportsScreen : Screen {
    @Composable
    override fun Content() {
        val reportsRepository: ReportsRepository = koinInject()
        var data by remember { mutableStateOf<ReportsData?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            try {
                data = reportsRepository.getReports()
            } catch (e: Exception) {
                errorMessage = e.message
            }
        }

        Scaffold(topBar = { TopAppBar(title = { Text("Informes de Rendimiento") }) }) { padding ->
            val current = data
            when {
                current == null && errorMessage == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMessage")
                }
                current != null -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    SectionTitle("Distribución de Ingresos por Servicio")
                    Spacer(modifier = Modifier.height(16.dp))
                    RevenuePieChart(current.revenueByService)

                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle("Rendimiento de Técnicos (Rating Promedio)")
                    Spacer(modifier = Modifier.height(16.dp))
                    TechBarChart(current.techPerformance)

                    Spacer(modifier = Modifier.height(32.dp))
                    SectionTitle("Resumen de Órdenes Completadas")
                    Spacer(modifier = Modifier.height(16.dp))
                    TechTable(current.techPerformance)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Primary)
}

private val ChartColors = listOf(AppColors.Secondary, AppColors.Primary, AppColors.Success, AppColors.Warning)

@Composable
private fun RevenuePieChart(data: List<RevenueByService>) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("No hay datos suficientes")
        }
        return
    }

    val total = data.sumOf { it.amount }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(140.dp)) {
                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweep = if (total > 0) (item.amount / total * 360.0).toFloat() else 0f
                    drawArc(
                        color = ChartColors[index % ChartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset.Zero,
                        size = Size(this.size.width, this.size.height),
                    )
                    startAngle += sweep
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                data.forEachIndexed { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(ChartColors[index % ChartColors.size], CircleShape),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${item.serviceType}  ${formatMoneyReport(item.amount)}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TechBarChart(data: List<TechPerformance>) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
            Text("No hay datos suficientes")
        }
        return
    }

    val maxRating = 5.0

    AppCard {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val slot = size.width / data.size
            val barWidth = (slot * 0.5f).coerceAtMost(40.dp.toPx())
            data.forEachIndexed { index, tech ->
                val ratio = (tech.averageRating / maxRating).coerceIn(0.0, 1.0).toFloat()
                val barHeight = ratio * size.height
                val x = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = AppColors.Secondary,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                )
            }
            // Línea base
            drawLine(
                color = AppColors.SurfaceBorder,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { tech ->
                Text(
                    text = tech.name.split(" ").firstOrNull() ?: tech.name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = AppColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun TechTable(data: List<TechPerformance>) {
    AppCard {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Técnico", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Órdenes", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Calificación", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            if (data.isEmpty()) {
                Text("Sin datos", color = AppColors.TextMuted, modifier = Modifier.padding(vertical = 8.dp))
            }
            data.forEach { tech ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tech.name, modifier = Modifier.weight(2f), fontSize = 13.sp)
                    Text(tech.completedOrders.toString(), modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatRating(tech.averageRating), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatMoneyReport(value: Double): String {
    val rounded = kotlin.math.round(value)
    return "$" + rounded.toString()
}

private fun formatRating(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10
    return rounded.toString()
}
