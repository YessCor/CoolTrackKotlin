package com.datasys.cooltrack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors

/** Un destino de navegación del menú que despliega el botón "+". */
data class AppNavTab(
    val label: String,
    val icon: ImageVector,
    val filledIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Una acción de creación del menú que despliega el botón "+". */
data class AppNavQuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Scaffold de shell sin barra inferior visible: su único control es un botón
 * circular "+" flotando abajo a la derecha, sobre el fondo de la pantalla.
 * Al tocarlo, todo el menú —primero los destinos de navegación ([tabs]) y
 * después las acciones de creación ([quickActions])— sube desde el botón en
 * cascada sobre un scrim, y el "+" rota hasta volverse una "X".
 *
 * El menú y el scrim se dibujan como hermanos del `Scaffold` (no dentro de
 * `bottomBar`), porque todo lo que se pinta fuera de los límites del slot
 * `bottomBar` queda sin recibir toques.
 */
@Composable
fun AppShellScaffold(
    tabs: List<AppNavTab>,
    quickActions: List<AppNavQuickAction> = emptyList(),
    content: @Composable (PaddingValues) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var barHeightPx by remember { mutableStateOf(0) }
    val barHeight = with(LocalDensity.current) { barHeightPx.toDp() }

    // El menú se dibuja de arriba hacia abajo, así que el último elemento es
    // el que queda pegado al "+" y debe entrar primero.
    val entryCount = tabs.size + quickActions.size
    fun delayFor(indexFromTop: Int) = (entryCount - 1 - indexFromTop) * 40

    // El botón se centra verticalmente dentro de la franja que reserva el `Scaffold`.
    val plusInset = ((barHeight - PlusButtonSize) / 2).coerceAtLeast(8.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // Franja transparente y vacía: no pinta nada, solo reserva el alto
            // (con el inset de la barra de gestos) para que el contenido de las
            // pantallas no quede tapado por el botón, que se dibuja aparte.
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.onSizeChanged { barHeightPx = it.height },
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    content = {},
                )
            },
            content = content,
        )

        // Overlay a pantalla completa: el scrim tapa todo —incluida la franja
        // inferior— y el botón "+" se dibuja encima, así nunca queda una banda
        // sin oscurecer.
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.Primary.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { expanded = false },
                )
            }

            AppPlusButton(
                expanded = expanded,
                onToggle = { expanded = !expanded },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = plusInset),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 24.dp, end = 20.dp, bottom = barHeight + 4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                tabs.forEachIndexed { index, tab ->
                    AppMenuEntry(visible = expanded, delayMillis = delayFor(index)) {
                        AppNavMenuPill(tab = tab) {
                            expanded = false
                            if (!tab.selected) tab.onClick()
                        }
                    }
                }
                quickActions.forEachIndexed { index, action ->
                    AppMenuEntry(visible = expanded, delayMillis = delayFor(tabs.size + index)) {
                        AppQuickActionPill(action = action) {
                            expanded = false
                            action.onClick()
                        }
                    }
                }
            }
        }
    }
}

/** Envuelve un elemento del menú en su entrada/salida escalonada. */
@Composable
private fun AppMenuEntry(visible: Boolean, delayMillis: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, delayMillis = delayMillis)) +
            slideInVertically(
                animationSpec = tween(280, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            ) { it / 2 + 60 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 + 60 },
    ) {
        content()
    }
}

private val PlusButtonSize = 56.dp

/** Botón circular "+" que abre y cierra el menú, rotando hasta una "X". */
@Composable
private fun AppPlusButton(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "navPlusRotation",
    )

    Surface(
        onClick = onToggle,
        modifier = modifier,
        shape = CircleShape,
        color = AppColors.Secondary,
        shadowElevation = 6.dp,
    ) {
        Box(modifier = Modifier.size(PlusButtonSize), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = if (expanded) "Cerrar menú" else "Abrir menú",
                tint = Color.White,
                modifier = Modifier.size(30.dp).graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

/**
 * Destino de navegación: superficie clara, y cian claro cuando es el activo.
 * Los colores del menú van todos opacos a propósito: cualquier alpha deja
 * pasar el scrim y el texto queda ilegible.
 */
@Composable
private fun AppNavMenuPill(tab: AppNavTab, onClick: () -> Unit) {
    AppMenuPill(
        label = tab.label,
        icon = if (tab.selected) tab.filledIcon else tab.icon,
        containerColor = if (tab.selected) Color(0xFFD6EEFB) else AppColors.Surface,
        iconBackground = if (tab.selected) AppColors.Secondary else Color(0xFFE0F2FE),
        iconTint = if (tab.selected) Color.White else AppColors.Secondary,
        labelColor = if (tab.selected) AppColors.SecondaryDark else AppColors.TextPrimary,
        onClick = onClick,
    )
}

/** Acción de creación: cian sólido, para separarla de los destinos. */
@Composable
private fun AppQuickActionPill(action: AppNavQuickAction, onClick: () -> Unit) {
    AppMenuPill(
        label = action.label,
        icon = action.icon,
        containerColor = AppColors.Primary,
        iconBackground = AppColors.PrimaryLight,
        iconTint = Color.White,
        labelColor = Color.White,
        onClick = onClick,
    )
}

@Composable
private fun AppMenuPill(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    iconBackground: Color,
    iconTint: Color,
    labelColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = labelColor,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
        }
    }
}
