package com.datasys.cooltrack.features.tech

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.OrderStatus
import com.datasys.cooltrack.features.notifications.NotificationsScreen
import com.datasys.cooltrack.models.ServiceOrder
import com.datasys.cooltrack.ui.components.AppCard
import com.datasys.cooltrack.ui.components.AppConfirmDialog
import com.datasys.cooltrack.ui.components.AppIcons
import com.datasys.cooltrack.ui.components.AppStatusBadge
import com.datasys.cooltrack.ui.components.AppTopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Home del técnico. Mismo lenguaje visual que [com.datasys.cooltrack.features.admin.AdminDashboardScreen]
 * (header con saludo, grilla de stats animada, accesos rápidos, lista reciente)
 * pero con datos y acciones propias del rol técnico: solo sus órdenes asignadas.
 */
class TechnicianDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val techRepository: TechRepository = koinInject()
        val authRepository: AuthRepository = koinInject()
        val scope = rememberCoroutineScope()

        val authState by authRepository.state.collectAsState()
        var jobs by remember { mutableStateOf<List<ServiceOrder>?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var showLogoutDialog by remember { mutableStateOf(false) }

        suspend fun refresh() {
            val userId = authState.user?.id ?: return
            isLoading = true
            jobs = try {
                techRepository.getAssignedJobs(userId)
            } catch (e: Exception) {
                emptyList()
            }
            isLoading = false
        }

        LaunchedEffect(authState.user?.id) { refresh() }

        Scaffold(
            topBar = {
                AppTopBar(
                    expandedHeight = 44.dp,
                    title = {
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextPrimary,
                        )
                    },
                    actions = {
                        IconButton(onClick = { navigator.push(NotificationsScreen()) }) {
                            Icon(imageVector = AppIcons.Notifications, contentDescription = "Notificaciones")
                        }
                    },
                )
            },
            containerColor = AppColors.SurfaceVariant,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(20.dp))
                    GreetingHeader(name = authState.user?.name)

                    Spacer(modifier = Modifier.height(24.dp))
                    SectionTitle("Resumen de Órdenes")
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        isLoading && jobs == null -> Box(
                            Modifier.fillMaxWidth().height(90.dp),
                            contentAlignment = Alignment.Center,
                        ) { CircularProgressIndicator(color = AppColors.Secondary) }

                        jobs != null -> JobsSummaryRow(jobs!!)
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionTitle("Accesos rápidos")
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActions(
                        navigator = navigator,
                        onLogout = { showLogoutDialog = true },
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionTitle("Próximas Órdenes")
                    Spacer(modifier = Modifier.height(4.dp))

                    when {
                        isLoading && jobs == null -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.Secondary)
                        }
                        jobs != null -> UpcomingJobsList(jobs!!, navigator)
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AppConfirmDialog(
                onDismissRequest = { showLogoutDialog = false },
                onConfirm = { scope.launch { authRepository.logout() } },
                title = "Cerrar Sesión",
                message = "¿Estás seguro de que deseas salir?",
                confirmText = "Cerrar Sesión",
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
}

@Composable
private fun GreetingHeader(name: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AppColors.Secondary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (name?.trim()?.firstOrNull() ?: 'T').uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.Secondary,
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Hola${name?.let { ", ${it.trim().substringBefore(' ')}" } ?: ""}",
                style = MaterialTheme.typography.headlineSmall,
                color = AppColors.TextPrimary,
            )
            Text(
                text = "¡Bienvenido de nuevo!",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
            )
        }
    }
}

private data class StatSpec(
    val title: String,
    val value: Int,
    val icon: ImageVector,
    val color: Color,
)

private val inProgressStatuses = setOf(
    OrderStatus.ASSIGNED,
    OrderStatus.ACCEPTED,
    OrderStatus.IN_TRANSIT,
    OrderStatus.IN_PROGRESS,
)

@Composable
private fun JobsSummaryRow(jobs: List<ServiceOrder>) {
    val pending = jobs.count { it.status == OrderStatus.PENDING }
    val inProgress = jobs.count { it.status in inProgressStatuses }
    val completed = jobs.count { it.status == OrderStatus.COMPLETED }

    val items = listOf(
        StatSpec("Pendientes", pending, AppIcons.Document, AppColors.Info),
        StatSpec("En proceso", inProgress, AppIcons.Clock, AppColors.Warning),
        StatSpec("Finalizadas", completed, AppIcons.CheckFilled, AppColors.Success),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEachIndexed { index, spec ->
            Box(modifier = Modifier.weight(1f)) {
                AnimatedStatCard(spec, index)
            }
        }
    }
}

@Composable
private fun AnimatedStatCard(spec: StatSpec, index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 },
    ) {
        val animatedValue = remember(spec.value) { Animatable(0f) }
        LaunchedEffect(spec.value) {
            delay(index * 70L)
            animatedValue.animateTo(spec.value.toFloat(), tween(700, easing = FastOutSlowInEasing))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.95f)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(20.dp))
                .background(AppColors.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(20.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(spec.color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = spec.icon, contentDescription = null, tint = spec.color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = animatedValue.value.toInt().toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppColors.TextPrimary,
            )
            Text(spec.title, fontSize = 11.sp, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val onTap: (Navigator) -> Unit,
)

@Composable
private fun QuickActions(navigator: Navigator, onLogout: () -> Unit) {
    val actions = listOf(
        QuickAction(AppIcons.Orders, "Órdenes", AppColors.Secondary) { it.push(TechnicianJobsScreen()) },
        QuickAction(AppIcons.Equipment, "Equipos", AppColors.Info) { it.push(TechnicianJobsScreen()) },
        QuickAction(AppIcons.Clock, "Historial", AppColors.StatusInProgress) { it.push(TechnicianJobsScreen()) },
        QuickAction(AppIcons.Logout, "Cerrar sesión", AppColors.Error) { onLogout() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowActions.forEach { action ->
                    Box(modifier = Modifier.weight(1f)) {
                        QuickActionTile(action, navigator)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, navigator: Navigator) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "tile-scale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(tileScale)
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(18.dp))
            .background(AppColors.Surface, RoundedCornerShape(18.dp))
            .border(1.dp, AppColors.SurfaceBorder, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interactionSource, indication = null) {
                if (action.label == "Cerrar sesión") action.onTap(navigator) else action.onTap(navigator)
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(action.color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = action.icon, contentDescription = null, tint = action.color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(action.label, style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary)
    }
}

@Composable
private fun UpcomingJobsList(jobs: List<ServiceOrder>, navigator: Navigator) {
    val upcoming = jobs.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }.take(5)
    if (upcoming.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(AppColors.Surface, RoundedCornerShape(14.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center,
        ) { Text("No tienes órdenes pendientes", color = AppColors.TextMuted) }
        return
    }
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        upcoming.forEachIndexed { index, job ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 60L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 6 },
            ) {
                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    onTap = { navigator.push(TechnicianJobDetailScreen(job.id)) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Orden #${job.orderNumber}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(job.address, color = AppColors.TextMuted, fontSize = 12.sp, maxLines = 1)
                        }
                        AppStatusBadge(status = job.status)
                    }
                }
            }
        }
    }
}
