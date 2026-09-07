package com.datasys.cooltrack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors
import com.datasys.cooltrack.core.AppGradients
import com.datasys.cooltrack.core.StatusBarIcons

/**
 * Header heroico: bloque a sangre completa con el gradiente de marca,
 * esquinas inferiores redondeadas y dos círculos decorativos difusos.
 * Es la firma del rediseño — se usa arriba de dashboards, detalles y el login.
 */
@Composable
fun AppHeroHeader(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    bottomExtra: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    StatusBarIcons(darkIcons = false)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(AppGradients.heroVertical())
            // Círculos decorativos difusos, pintados sin afectar el layout.
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width * 0.92f, size.height * 0.12f),
                )
                drawCircle(
                    color = AppColors.Accent.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.08f, size.height * 0.95f),
                )
            },
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = bottomExtra),
            ) {
                if (onBack != null || actions != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(AppIcons.ArrowBack, contentDescription = "Volver", tint = Color.White)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (actions != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                } else {
                    Spacer(Modifier.height(12.dp))
                }
                Column(modifier = Modifier.padding(start = if (onBack != null) 4.dp else 0.dp), content = content)
            }
        }
    }
}

/**
 * Scaffold heroico: [AppHeroHeader] arriba y, debajo, una "hoja" de contenido
 * con esquinas superiores redondeadas que sube ~24dp sobre el header. El
 * contenido scrollea dentro de la hoja.
 */
@Composable
fun AppHeroScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    heroContent: (@Composable ColumnScope.() -> Unit)? = null,
    scrollable: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            AppHeroHeader(onBack = onBack, actions = actions, bottomExtra = 44.dp) {
                if (heroContent != null) {
                    heroContent()
                } else {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            // Hoja de contenido, superpuesta.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(AppColors.Background),
            ) {
                val base = Modifier.fillMaxSize().padding(contentPadding)
                if (scrollable) {
                    Column(base.verticalScroll(rememberScrollState()), content = content)
                } else {
                    Column(base, content = content)
                }
            }
        }

        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomEnd) {
            floatingActionButton()
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { snackbarHost() }
    }
}

/**
 * Header hero COMPACTO para pantallas de lista: barra de gradiente baja con
 * título, subtítulo opcional y un slot a la derecha (chip de conteo, filtro).
 */
@Composable
fun AppCompactHero(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    AppHeroHeader(modifier = modifier, onBack = onBack, bottomExtra = 26.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.78f))
                }
            }
            if (trailing != null) Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

/** Chip translúcido para poner en el `trailing` de [AppCompactHero] (ej. conteo). */
@Composable
fun AppHeroChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Scaffold de lista con hero compacto: el `content` es un `LazyListScope`
 * (para poder usar `LazyColumn` de verdad). La hoja de contenido sube sobre
 * el header.
 */
@Composable
fun AppHeroListScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    heroTrailing: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
    listContent: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        Column(Modifier.fillMaxSize()) {
            AppCompactHero(title = title, subtitle = subtitle, onBack = onBack, trailing = heroTrailing)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-22).dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(AppColors.Background),
            ) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = listContent,
                )
            }
        }
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomEnd) { floatingActionButton() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) { snackbarHost() }
    }
}

/**
 * Pantalla de lista completa del rediseño: hero compacto + hoja con
 * `LazyColumn`, y manejo interno de los estados carga / error / vacío
 * (skeletons, [AppErrorState], [AppEmptyState]). El `items` recibe la lista
 * ya resuelta y define cada fila.
 */
@Composable
fun <T> AppListScreen(
    title: String,
    data: List<T>?,
    error: String?,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector,
    emptyTitle: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    emptyMessage: String? = null,
    emptyAction: (@Composable () -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    heroTrailing: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    loadingCount: Int = 6,
    items: androidx.compose.foundation.lazy.LazyListScope.(List<T>) -> Unit,
) {
    val autoSubtitle = subtitle ?: data?.let {
        if (it.isEmpty()) null else "${it.size} ${if (it.size == 1) "resultado" else "resultados"}"
    }
    AppHeroListScaffold(
        title = title,
        modifier = modifier,
        subtitle = autoSubtitle,
        onBack = onBack,
        heroTrailing = heroTrailing,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
    ) {
        when {
            error != null && data == null -> item {
                AppErrorState(message = error, onRetry = onRetry, modifier = Modifier.padding(top = 32.dp))
            }
            data == null -> items(loadingCount) { AppSkeletonListCard(Modifier.appEnter(it)) }
            data.isEmpty() -> item {
                AppEmptyState(icon = emptyIcon, title = emptyTitle, message = emptyMessage, action = emptyAction, modifier = Modifier.padding(top = 24.dp))
            }
            else -> items(data)
        }
    }
}

/** Avatar circular translúcido para el header hero (inicial o ícono). */
@Composable
fun AppHeroAvatar(initial: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial.take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** Número que cuenta hacia arriba al aparecer. */
@Composable
fun AppAnimatedCount(
    value: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = AppColors.TextPrimary,
    fontWeight: FontWeight = FontWeight.ExtraBold,
    durationMillis: Int = 750,
    startDelayMillis: Int = 0,
) {
    val anim = remember(value) { Animatable(0f) }
    LaunchedEffect(value) {
        anim.snapTo(0f)
        anim.animateTo(value.toFloat(), tween(durationMillis, delayMillis = startDelayMillis))
    }
    Text(
        text = anim.value.toInt().toString(),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
    )
}

/** FAB de marca: círculo índigo con sombra teñida, entra con "pop", hunde al presionar. */
@Composable
fun AppFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .appPop(delayMillis = 250)
            .size(58.dp)
            .shadow(14.dp, CircleShape, ambientColor = AppColors.ShadowTint, spotColor = AppColors.ShadowTint)
            .clip(CircleShape)
            .background(AppGradients.accent())
            .pressable(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Punto de estado que late (para "activo", "en línea", "sincronizando"). */
@Composable
fun AppLiveDot(color: Color = AppColors.Success, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(color).pulse(minAlpha = 0.35f))
}
