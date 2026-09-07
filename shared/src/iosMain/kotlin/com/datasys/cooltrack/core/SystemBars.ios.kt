package com.datasys.cooltrack.core

import androidx.compose.runtime.Composable

@Composable
actual fun StatusBarIcons(darkIcons: Boolean) {
    // no-op: en iOS el status bar se controla vía UIViewController / Info.plist.
}
