package com.datasys.cooltrack.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hvac
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubble
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Hvac
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Icon as Material3Icon

/**
 * Equivalente a components/icons.dart.
 *
 * Flutter usa `IconData` (una fuente de íconos + code point); Compose usa
 * `ImageVector`. Cada constante de `AppIcons` original se mapea 1:1 a su
 * símbolo más cercano dentro de `material-icons-extended` (ya declarado en
 * `shared/build.gradle.kts`). Donde el original distinguía variante
 * "outlined" vs "filled" con el mismo nombre base (p. ej.
 * `dashboard_outlined` / `dashboard`), acá se usa `Icons.Outlined.X` /
 * `Icons.Filled.X` respectivamente.
 */
object AppIcons {
    val Dashboard: ImageVector = Icons.Outlined.Dashboard
    val DashboardFilled: ImageVector = Icons.Filled.Dashboard

    val Clients: ImageVector = Icons.Outlined.People
    val ClientsFilled: ImageVector = Icons.Filled.People

    val Technicians: ImageVector = Icons.Outlined.Engineering
    val TechniciansFilled: ImageVector = Icons.Filled.Engineering

    val Orders: ImageVector = Icons.Filled.ListAlt
    val OrdersFilled: ImageVector = Icons.Filled.ListAlt

    val Quotes: ImageVector = Icons.Outlined.ReceiptLong
    val QuotesFilled: ImageVector = Icons.Filled.ReceiptLong

    val Equipment: ImageVector = Icons.Outlined.Hvac
    val EquipmentFilled: ImageVector = Icons.Filled.Hvac

    val Home: ImageVector = Icons.Outlined.Home
    val HomeFilled: ImageVector = Icons.Filled.Home

    val Profile: ImageVector = Icons.Outlined.Person
    val ProfileFilled: ImageVector = Icons.Filled.Person

    val Services: ImageVector = Icons.Outlined.Handyman
    val ServicesFilled: ImageVector = Icons.Filled.Handyman

    val Settings: ImageVector = Icons.Outlined.Settings
    val SettingsFilled: ImageVector = Icons.Filled.Settings

    val Notifications: ImageVector = Icons.Outlined.Notifications
    val NotificationsFilled: ImageVector = Icons.Filled.Notifications

    val Search: ImageVector = Icons.Filled.Search
    val Filter: ImageVector = Icons.Filled.FilterList
    val Sort: ImageVector = Icons.Filled.Sort

    val Add: ImageVector = Icons.Filled.Add
    val Edit: ImageVector = Icons.Filled.Edit
    val Delete: ImageVector = Icons.Outlined.Delete
    val View: ImageVector = Icons.Outlined.Visibility

    val Camera: ImageVector = Icons.Outlined.CameraAlt
    val Photo: ImageVector = Icons.Outlined.Photo
    val Signature: ImageVector = Icons.Outlined.Draw

    val Location: ImageVector = Icons.Outlined.LocationOn
    val Map: ImageVector = Icons.Outlined.Map

    val Calendar: ImageVector = Icons.Outlined.CalendarToday
    val Clock: ImageVector = Icons.Filled.AccessTime

    val Phone: ImageVector = Icons.Outlined.Phone
    val Email: ImageVector = Icons.Outlined.Email
    val Whatsapp: ImageVector = Icons.Outlined.ChatBubble

    val Download: ImageVector = Icons.Outlined.Download
    val Share: ImageVector = Icons.Outlined.Share
    val Print: ImageVector = Icons.Outlined.Description

    val Check: ImageVector = Icons.Outlined.CheckCircle
    val CheckFilled: ImageVector = Icons.Filled.CheckCircle
    val Error: ImageVector = Icons.Outlined.Error
    val Warning: ImageVector = Icons.Outlined.WarningAmber
    val Info: ImageVector = Icons.Outlined.Info

    val ChevronRight: ImageVector = Icons.Filled.ChevronRight
    val ChevronLeft: ImageVector = Icons.Filled.ChevronLeft
    val ArrowBack: ImageVector = Icons.Filled.ArrowBack
    val ArrowForward: ImageVector = Icons.Filled.ArrowForward

    val Menu: ImageVector = Icons.Filled.Menu
    val More: ImageVector = Icons.Filled.MoreVert
    val Close: ImageVector = Icons.Filled.Close

    val Wifi: ImageVector = Icons.Filled.Wifi
    val WifiOff: ImageVector = Icons.Filled.WifiOff
    val Sync: ImageVector = Icons.Filled.Sync
    val SyncProblem: ImageVector = Icons.Filled.SyncProblem
    val Cloud: ImageVector = Icons.Outlined.Cloud
    val CloudDone: ImageVector = Icons.Outlined.CloudDone

    val Dollar: ImageVector = Icons.Filled.AttachMoney
    val Document: ImageVector = Icons.Outlined.Description
    val Folder: ImageVector = Icons.Outlined.Folder

    val Technician: ImageVector = Icons.Filled.SupportAgent
    val User: ImageVector = Icons.Outlined.AccountCircle
    val Users: ImageVector = Icons.Outlined.Group

    val Cooling: ImageVector = Icons.Filled.AcUnit
    val Heating: ImageVector = Icons.Filled.Whatshot
    val Maintenance: ImageVector = Icons.Outlined.Handyman
    val Repair: ImageVector = Icons.Filled.Construction

    // Usados puntualmente por AppStatusBadge / AppQuoteStatusBadge (no
    // formaban parte de la clase AppIcons original, pero se agrupan acá
    // para no duplicar imports de material-icons-extended en cada archivo).
    val HourglassEmpty: ImageVector = Icons.Outlined.HourglassEmpty
    val DirectionsCar: ImageVector = Icons.Filled.DirectionsCar
    val Build: ImageVector = Icons.Filled.Construction
    val Cancel: ImageVector = Icons.Filled.Cancel
    val Send: ImageVector = Icons.Filled.Send
    val TimerOff: ImageVector = Icons.Filled.TimerOff
    val Clear: ImageVector = Icons.Filled.Close

    // Usados puntualmente por las pantallas de admin (módulo 5b): tampoco
    // formaban parte de la clase AppIcons original de Flutter (ahí cada
    // pantalla instanciaba su propio `Icons.xxx` suelto), se agrupan acá
    // por el mismo motivo que el bloque anterior.
    val Refresh: ImageVector = Icons.Filled.Refresh
    val Logout: ImageVector = Icons.Filled.Logout
    val PersonAdd: ImageVector = Icons.Filled.PersonAdd
    val Reports: ImageVector = Icons.Filled.BarChart
    val Catalog: ImageVector = Icons.Filled.ListAlt
    val Star: ImageVector = Icons.Filled.Star
    val Lock: ImageVector = Icons.Filled.Lock
}

/**
 * Equivalente a AppIcon (StatelessWidget) en components/icons.dart:
 * un ícono simple, opcionalmente tappable.
 */
@Composable
fun AppIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    tint: Color? = null,
    onTap: (() -> Unit)? = null,
) {
    var iconModifier = modifier
    if (size != null) iconModifier = iconModifier.size(size)
    if (onTap != null) iconModifier = iconModifier.clickable { onTap() }

    Material3Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = iconModifier,
        tint = tint ?: Color.Unspecified,
    )
}
