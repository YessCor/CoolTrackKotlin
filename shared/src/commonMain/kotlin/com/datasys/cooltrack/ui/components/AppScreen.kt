package com.datasys.cooltrack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors

/**
 * Escala de espaciado única de la app. Reemplaza el uso disperso de
 * `Spacer(Modifier.height(8/12/16.dp))` y paddings sueltos por una referencia
 * común, para que el ritmo vertical sea el mismo en todas las pantallas.
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Padding de página estándar para el contenido scrolleable de una pantalla. */
    val screen: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
}

/** Colores estándar del [AppTopBar] en toda la app: superficie clara, título oscuro. */
@Composable
fun appTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = AppColors.Surface,
    scrolledContainerColor = AppColors.Surface,
    titleContentColor = AppColors.TextPrimary,
    navigationIconContentColor = AppColors.TextSecondary,
    actionIconContentColor = AppColors.TextSecondary,
)

/**
 * Scaffold estándar de pantalla: [AppTopBar] con colores unificados, una
 * hairline bajo la barra, fondo neutro y —opcionalmente— flecha de volver.
 * Sustituye el `Scaffold { AppTopBar(...) }` repetido (y despareja) de cada
 * pantalla.
 */
@Composable
fun AppScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    containerColor: Color = AppColors.SurfaceVariant,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        topBar = {
            Column {
                AppTopBar(
                    expandedHeight = 48.dp,
                    colors = appTopBarColors(),
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = AppColors.TextPrimary,
                        )
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(AppIcons.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    actions = actions,
                )
                Divider(thickness = 1.dp, color = AppColors.SurfaceBorder)
            }
        },
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Shimmer / skeletons de carga
// ---------------------------------------------------------------------------

/** Fondo con barrido de brillo animado, para placeholders de carga. */
fun Modifier.shimmer(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmer-progress",
    )
    val base = AppColors.SurfaceVariant
    val highlight = AppColors.SurfaceBorder.copy(alpha = 0.6f)
    val start = -600f + progress * 1800f
    this
        .clip(shape)
        .background(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = androidx.compose.ui.geometry.Offset(start, 0f),
                end = androidx.compose.ui.geometry.Offset(start + 600f, 0f),
            ),
        )
}

/** Bloque rectangular con shimmer (barra de texto, avatar, etc.). */
@Composable
fun AppSkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    width: Dp? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val sized = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Box(sized.height(height).shimmer(shape))
}

/** Tarjeta placeholder que imita la anatomía de una fila de lista (avatar + 2 líneas). */
@Composable
fun AppSkeletonListCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).shimmer(CircleShape))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppSkeletonBox(width = 160.dp, height = 13.dp)
            AppSkeletonBox(width = 100.dp, height = 11.dp)
        }
    }
}

/** Lista de skeletons con el padding de página estándar. Reemplaza al spinner suelto. */
@Composable
fun AppLoadingList(
    modifier: Modifier = Modifier,
    count: Int = 6,
    contentPadding: PaddingValues = Spacing.screen,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        items(count) { AppSkeletonListCard() }
    }
}

// ---------------------------------------------------------------------------
// Estado de error
// ---------------------------------------------------------------------------

/** Estado de error a página completa, con ícono en badge y reintento opcional. */
@Composable
fun AppErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Algo salió mal",
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(88.dp).background(AppColors.Error.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Warning, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(Spacing.xl))
            AppButton(label = "Reintentar", icon = AppIcons.Refresh, variant = AppButtonVariant.OUTLINE, onPressed = onRetry)
        }
    }
}

/**
 * Envoltura para el patrón "cargando / error / vacío / contenido" que hoy
 * cada lista implementa a mano con un `when` distinto. `data == null` con
 * `error == null` es carga; el resto se decide con las lambdas.
 */
@Composable
fun <T> AppAsyncContent(
    data: List<T>?,
    error: String?,
    emptyIcon: ImageVector,
    emptyTitle: String,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
    emptyAction: (@Composable () -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    loadingCount: Int = 6,
    content: @Composable (List<T>) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        when {
            error != null && data == null -> AppErrorState(
                message = error,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
            data == null -> AppLoadingList(count = loadingCount)
            data.isEmpty() -> AppEmptyState(
                icon = emptyIcon,
                title = emptyTitle,
                message = emptyMessage,
                action = emptyAction,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> content(data)
        }
    }
}

// ---------------------------------------------------------------------------
// Piezas de tarjeta reutilizables
// ---------------------------------------------------------------------------

/** Ícono dentro de un badge circular teñido — el patrón "leading" de casi toda fila. */
@Composable
fun AppLeadingIcon(
    icon: ImageVector,
    tint: Color = AppColors.Secondary,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size).background(tint.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** Chip compacto teñido (estado, etiqueta corta). */
@Composable
fun AppTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Entrada escalonada (fade + subida) para filas de lista, en función de su
 * índice. Reemplaza el `AnimatedVisibility` + `remember { mutableStateOf }`
 * manual que solo tenían algunas pantallas.
 */
fun Modifier.staggeredItem(index: Int, stepMillis: Int = 45): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 320,
                delayMillis = (index.coerceAtMost(10)) * stepMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }
    graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 24f
    }
}

// ---------------------------------------------------------------------------
// Formato
// ---------------------------------------------------------------------------

/** "$1.234,50" a partir de un monto — alias de [com.datasys.cooltrack.models.formatMoney]. */
fun formatCurrency(amount: Double): String = com.datasys.cooltrack.models.formatMoney(amount)

/** Fecha corta legible a partir de un ISO-8601 ("2026-09-07T..."→ "07 sep 2026"). */
fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val datePart = iso.substringBefore('T')
    val bits = datePart.split("-")
    if (bits.size < 3) return datePart
    val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    val m = bits[1].toIntOrNull()?.let { months.getOrNull(it - 1) } ?: bits[1]
    return "${bits[2].padStart(2, '0')} $m ${bits[0]}"
}

/** Aviso teñido con ícono — para notas informativas dentro de una pantalla. */
@Composable
fun AppInfoBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.Info,
    color: Color = AppColors.Info,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Tarjeta contenedora de un grupo de campos de formulario, con un título de
 * sección opcional arriba. Da profundidad y agrupa visualmente los inputs.
 */
@Composable
fun AppFormSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(modifier) {
        if (title != null) {
            AppSectionTitle(title)
            Spacer(Modifier.height(Spacing.md))
        }
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg), content = content)
        }
    }
}

/** Título de sección unificado (antes: `SectionTitle` local vs `AppSectionHeader`). */
@Composable
fun AppSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
        trailing?.invoke()
    }
}
