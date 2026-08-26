package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.datasys.cooltrack.core.AppColors

/**
 * Equivalente a AppInput en components/input.dart.
 *
 * Dart usaba `TextEditingController` (mutable, imperativo); en Compose el
 * patrón idiomático es "state hoisting": el `value` vive afuera (en el
 * `ScreenModel`/estado local de la pantalla) y se pasa junto con
 * `onValueChange`, en vez de un controller. `validator`/`errorText` se
 * combinan en un solo `errorText` ya resuelto por quien llama (igual que
 * hacían las pantallas originales al mostrar `errorText` explícito tras
 * validar en el submit).
 */
@Composable
fun AppInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hint: String? = null,
    errorText: String? = null,
    obscureText: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    maxLines: Int = 1,
    maxLength: Int? = null,
    prefixIcon: ImageVector? = null,
    suffix: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    autofocus: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        val effectiveFocusRequester = focusRequester ?: if (autofocus) remember { FocusRequester() } else null
        val fieldModifier = if (effectiveFocusRequester != null) {
            Modifier.fillMaxWidth().focusRequester(effectiveFocusRequester)
        } else {
            Modifier.fillMaxWidth()
        }
        if (autofocus && effectiveFocusRequester != null) {
            LaunchedEffect(Unit) { effectiveFocusRequester.requestFocus() }
        }

        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                if (maxLength == null || new.length <= maxLength) onValueChange(new)
            },
            modifier = fieldModifier,
            enabled = enabled,
            placeholder = if (hint != null) {
                { Text(text = hint, color = AppColors.TextMuted) }
            } else null,
            isError = errorText != null,
            supportingText = if (errorText != null) {
                { Text(text = errorText) }
            } else null,
            leadingIcon = if (prefixIcon != null) {
                { Icon(imageVector = prefixIcon, contentDescription = null, tint = AppColors.TextMuted) }
            } else null,
            trailingIcon = suffix,
            visualTransformation = if (obscureText) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction?.invoke() },
                onNext = { onImeAction?.invoke() },
                onGo = { onImeAction?.invoke() },
                onSearch = { onImeAction?.invoke() },
                onSend = { onImeAction?.invoke() },
            ),
            maxLines = maxLines,
            singleLine = maxLines == 1,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.Secondary,
                unfocusedBorderColor = AppColors.SurfaceBorder,
                errorBorderColor = AppColors.Error,
                focusedContainerColor = AppColors.Surface,
                unfocusedContainerColor = AppColors.Surface,
                cursorColor = AppColors.Secondary,
            ),
        )
    }
}

/** Equivalente a AppSearchInput en components/input.dart. */
@Composable
fun AppSearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "Buscar...",
    onClear: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(text = hint, color = AppColors.TextMuted) },
        leadingIcon = { Icon(imageVector = AppIcons.Search, contentDescription = null, tint = AppColors.TextMuted) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = {
                    onValueChange("")
                    onClear?.invoke()
                }) {
                    Icon(imageVector = AppIcons.Clear, contentDescription = null, tint = AppColors.TextMuted)
                }
            }
        } else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Secondary,
            unfocusedBorderColor = AppColors.SurfaceBorder,
            focusedContainerColor = AppColors.SurfaceVariant,
            unfocusedContainerColor = AppColors.SurfaceVariant,
            cursorColor = AppColors.Secondary,
        ),
    )
}
