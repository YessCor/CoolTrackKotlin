package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors

/**
 * Equivalente a AppModal en components/modal.dart.
 *
 * El original exponía `AppModal.show(context, ...)` (imperativo, sobre
 * `showModalBottomSheet`). En Compose el patrón idiomático es que la
 * pantalla que llama mantenga un `var showSheet by remember { mutableStateOf(false) }`
 * y renderice este composable condicionalmente — `onDismissRequest` es el
 * reemplazo directo del botón de cerrar (`Navigator.pop()`) y del
 * `isDismissible` original (que acá se controla dejando `onDismissRequest`
 * en `{}` si no se quiere permitir cerrar tocando afuera).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = AppColors.Surface,
    ) {
        Column {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(imageVector = AppIcons.Close, contentDescription = null, tint = AppColors.TextMuted)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) { content() }

            if (actions != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) { actions() }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Equivalente a AppConfirmDialog en components/modal.dart, renderizado
 * como `AlertDialog` de Material3. El original tenía un `static Future<bool>
 * show(...)` que devolvía el resultado por await; acá se reemplaza con
 * `onConfirm`/`onDismissRequest` explícitos (mismo patrón reactivo que el
 * resto del proyecto).
 */
@Composable
fun AppConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirmar",
    cancelText: String = "Cancelar",
    confirmColor: Color? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = AppColors.Surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary) },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    onConfirm()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor ?: AppColors.Secondary,
                    contentColor = Color.White,
                ),
            ) { Text(text = confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = cancelText, color = AppColors.TextSecondary) }
        },
    )
}
