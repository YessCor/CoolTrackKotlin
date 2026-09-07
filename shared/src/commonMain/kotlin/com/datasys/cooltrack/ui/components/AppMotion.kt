package com.datasys.cooltrack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Sistema de movimiento del rediseño. Todo lo táctil usa `Modifier.pressable`
 * (hunde + reacciona con resorte), las apariciones usan `appEnter`, y los
 * elementos "vivos" (estado activo, sincronizando) usan `pulse`.
 */

/** Curva estándar de la app para transiciones cortas. */
val AppEasing = FastOutSlowInEasing

/**
 * Reacción de presión: escala a ~0.96 mientras se mantiene, vuelve con un
 * pequeño rebote. Incluye feedback háptico opcional. Reemplaza a
 * `Modifier.clickable` en tarjetas, tiles y botones custom.
 */
fun Modifier.pressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val hapticFeedback = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "pressable-scale",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    if (haptic) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tryAwaitRelease()
                    pressed = false
                },
                onTap = { onClick() },
            )
        }
}

/** Igual que [pressable] pero para un `InteractionSource` ya existente (botones Material). */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.94f): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press-scale",
    )
    return scale
}

/**
 * Aparición de entrada: fade + subida + un toque de escala, escalonada por
 * `index`. Es el reemplazo unificado de los `AnimatedVisibility` sueltos.
 */
fun Modifier.appEnter(
    index: Int = 0,
    stepMillis: Int = 55,
    fromScale: Float = 0.94f,
    slidePx: Float = 28f,
): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 420,
                delayMillis = index.coerceAtMost(12) * stepMillis,
                easing = AppEasing,
            ),
        )
    }
    graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * slidePx
        val s = fromScale + (1f - fromScale) * anim.value
        scaleX = s
        scaleY = s
    }
}

/** Aparición "pop" con rebote — para elementos únicos y destacados (íconos hero, FAB). */
fun Modifier.appPop(delayMillis: Int = 0): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(500, delayMillis = delayMillis, easing = EaseOutBack))
    }
    graphicsLayer {
        alpha = anim.value.coerceIn(0f, 1f)
        scaleX = anim.value
        scaleY = anim.value
    }
}

/** Latido sutil e infinito — para indicadores de estado "vivo". */
fun Modifier.pulse(minAlpha: Float = 0.45f, periodMillis: Int = 1300): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = minAlpha,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = AppEasing), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    graphicsLayer { this.alpha = alpha }
}

/** Rotación infinita — spinners/íconos de refresco. */
fun Modifier.spin(active: Boolean, periodMillis: Int = 850): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spin-angle",
    )
    graphicsLayer { rotationZ = if (active) angle else 0f }
}
