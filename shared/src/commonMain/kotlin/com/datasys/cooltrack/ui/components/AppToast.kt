package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Equivalente a AppToastType en components/toast.dart. */
enum class AppToastType { INFO, SUCCESS, ERROR, WARNING }

private data class AppToastVisuals(
    val text: String,
    val type: AppToastType,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val duration: SnackbarDuration = SnackbarDuration.Short
    override val message: String = text
    override val withDismissAction: Boolean = false
}

/**
 * Equivalente a AppToast (clase estática) en components/toast.dart.
 *
 * Flutter mostraba el toast sobre un `BuildContext` global vía
 * `ScaffoldMessenger.of(context)`. En Compose no hay un context global
 * equivalente: cada `Scaffold` necesita su propio `SnackbarHostState`. Este
 * `AppToastState` es ese holder — se crea una vez por pantalla con
 * `rememberAppToastState()`, se conecta al `Scaffold(snackbarHost = ...)`
 * con `AppToastHost(state)`, y se dispara igual que antes con
 * `state.showSuccess(...)`, `state.showError(...)`, etc.
 */
class AppToastState(val snackbarHostState: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, type: AppToastType = AppToastType.INFO) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(AppToastVisuals(message, type))
        }
    }

    fun showSuccess(message: String) = show(message, AppToastType.SUCCESS)
    fun showError(message: String) = show(message, AppToastType.ERROR)
    fun showWarning(message: String) = show(message, AppToastType.WARNING)
    fun showInfo(message: String) = show(message, AppToastType.INFO)
}

@Composable
fun rememberAppToastState(): AppToastState {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(snackbarHostState, scope) { AppToastState(snackbarHostState, scope) }
}

private fun colorFor(type: AppToastType): Color = when (type) {
    AppToastType.INFO -> AppColors.Secondary
    AppToastType.SUCCESS -> AppColors.Success
    AppToastType.ERROR -> AppColors.Error
    AppToastType.WARNING -> AppColors.Warning
}

private fun iconFor(type: AppToastType) = when (type) {
    AppToastType.INFO -> AppIcons.Info
    AppToastType.SUCCESS -> AppIcons.Check
    AppToastType.ERROR -> AppIcons.Error
    AppToastType.WARNING -> AppIcons.Warning
}

/**
 * Va en `Scaffold(snackbarHost = { AppToastHost(toastState) })`. Reemplaza
 * el `SnackBar` con ícono + color por tipo que armaba `AppToast.show`.
 */
@Composable
fun AppToastHost(state: AppToastState) {
    SnackbarHost(hostState = state.snackbarHostState) { data ->
        val visuals = data.visuals
        val type = (visuals as? AppToastVisuals)?.type ?: AppToastType.INFO
        Snackbar(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = colorFor(type),
            contentColor = Color.White,
        ) {
            Row {
                Icon(imageVector = iconFor(type), contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = visuals.message)
            }
        }
    }
}

/**
 * Equivalente a AppLoadingOverlay en components/toast.dart.
 *
 * El original también tenía un `show(context)`/`hide(context)` estáticos
 * sobre un `Dialog` global; ese patrón imperativo se reemplaza por completo
 * con el parámetro reactivo `isLoading` de este wrapper (la pantalla que
 * llama solo cambia su `var isLoading by remember { mutableStateOf(false) }`,
 * igual que ya se hace en todo el resto del proyecto con `StateFlow`).
 */
@Composable
fun AppLoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(AppColors.Surface, RoundedCornerShape(20.dp))
                        .padding(24.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppColors.Secondary)
                        if (message != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = message)
                        }
                    }
                }
            }
        }
    }
}
