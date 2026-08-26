package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Barra superior compacta, con control directo del alto y el espaciado
 * vertical. Reemplaza a `androidx.compose.material3.TopAppBar`: en este
 * proyecto, el `TopAppBar` de Material3 dejaba un espacio muerto grande
 * entre la barra de estado y el título incluso reduciendo `expandedHeight`
 * (su fórmula interna de centrado no respondía linealmente a ese parámetro
 * en la versión resuelta de Compose Multiplatform). Acá el título se centra
 * dentro de exactamente `expandedHeight` mediante un `Row` simple, sin
 * relleno adicional oculto.
 *
 * Acepta el mismo [TopAppBarColors] que ya arma cada pantalla con
 * `TopAppBarDefaults.topAppBarColors(...)`, así que migrar una pantalla es
 * solo cambiar el nombre de la función.
 */
@Composable
fun AppTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = 44.dp,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    Surface(color = colors.containerColor, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = expandedHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.navigationIconContentColor) {
                navigationIcon()
            }
            Box(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                CompositionLocalProvider(LocalContentColor provides colors.titleContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                        title()
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides colors.actionIconContentColor) {
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}
