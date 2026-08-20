# Cooltrack: de Flutter a Kotlin Multiplatform (KMP)

## Qué se hizo en esta primera entrega (Módulo 1: base del proyecto)

- Proyecto Gradle multiplataforma (`shared` compilando a Android + iOS).
- `core/Constants.kt`: todos los enums y mapas de `lib/core/constants.dart`
  (roles, estados de orden, tipos de equipo, estados de cotización, transiciones
  permitidas por rol).
- `core/AppConfig.kt` (+ `.android.kt` / `.ios.kt`): reemplaza `flutter_dotenv`
  con `expect`/`actual`, inyectando Supabase/Cloudinary por plataforma en vez
  de empaquetar un `.env` en el binario.
- Todos los modelos de `lib/models/*.dart` migrados a `data class` con
  `kotlinx.serialization` (`User`, `Client`, `Equipment`, `Media`,
  `AppNotification`, `Quote`/`QuoteItem`, `ServiceCatalog`, `ServiceOrder`,
  `TechnicianLocation`, `DashboardStats`).

## Equivalencias de librerías (pubspec.yaml → Kotlin)

| Flutter                    | Kotlin Multiplatform                              |
|-----------------------------|----------------------------------------------------|
| flutter_riverpod            | `ViewModel` + `StateFlow`/`MutableStateFlow` (o Voyager ScreenModel) |
| go_router                   | Voyager Navigator                                   |
| dio                         | Ktor Client                                         |
| hive / hive_flutter         | SQLDelight (persistencia offline tipada)            |
| flutter_secure_storage      | multiplatform-settings (con Keychain/EncryptedSharedPreferences) |
| geolocator                  | `play-services-location` (Android) + `CoreLocation` vía expect/actual (iOS) |
| image_picker                | `PhotoPicker` (Android) / `PHPicker` (iOS) vía expect/actual |
| supabase_flutter             | supabase-kt (postgrest-kt, gotrue-kt, realtime-kt, storage-kt) |
| firebase_messaging           | Firebase Kotlin SDK (KMP) o servicios nativos por plataforma |
| signature                   | Componente Compose dibujado con `Canvas` + `Path`   |
| pdf / printing               | `PdfDocument` nativo Android / `PDFKit` iOS, vía expect/actual, o librería KMP como `korge/korim` |
| json_annotation / freezed    | `kotlinx.serialization` + `data class` (el `copy()` de Kotlin ya reemplaza a `copyWith`) |
| Widgets de Flutter (UI)      | Compose Multiplatform (`compose.material3`, compartido en `commonMain`) |

## Decisiones tomadas

- **Compose Multiplatform** para compartir también la UI (no solo lógica),
  ya que es el análogo más cercano al modelo "un solo árbol de widgets" de
  Flutter. Si preferís UI 100% nativa (SwiftUI en iOS + Compose puro en
  Android), lo ajustamos — es un cambio solo en el módulo `androidApp`/`iosApp`,
  no afecta lo ya migrado en `shared`.
- `DateTime` de Dart → `kotlinx.datetime.Instant` (o `LocalDate` cuando el
  campo no lleva hora, como `installation_date`).
- Los `copyWith(...)` manuales de Dart desaparecen: al ser `data class`,
  Kotlin genera `copy()` automáticamente.

## Módulo 2: ApiClient, almacenamiento seguro, auth y navegación (agregado)

- `core/ApiClient.kt`: reemplaza `dio` con **Ktor**. Mismo contrato que el
  original (`get/post/put/patch/delete/uploadFile`), mismo `ApiException`,
  mismo interceptor de `Authorization: Bearer`. El engine HTTP se resuelve
  solo por plataforma (OkHttp en Android, Darwin en iOS) gracias a las
  dependencias ya declaradas en `shared/build.gradle.kts`.
- `core/SecureStorage.kt` (+ `.android.kt` / `.ios.kt`): reemplaza
  `flutter_secure_storage` con `EncryptedSharedPreferences` en Android y
  Keychain en iOS vía `expect`/`actual`. **Importante**: en Android hay que
  llamar una vez a `SecureStorageInitializer.init(context)` desde tu
  `Application.onCreate()` (Flutter no lo pedía porque el plugin ya tenía el
  Context; en Kotlin puro sí hace falta pasarlo explícitamente).
- `auth/AuthRepository.kt`: reemplaza `AuthNotifier`/`authProvider` de
  Riverpod. Misma lógica exacta (login, logout, register, refreshUser,
  restaurar sesión guardada), pero expuesta como `StateFlow<AuthState>` en
  vez de `StateNotifier`. Cualquier capa de UI (Compose, o incluso SwiftUI si
  se separa la UI) puede observar este flujo.
- `navigation/AppNavigation.kt`: reemplaza `core/router.dart` (go_router).
  Las rutas por string (`/admin`, `/technician/job/:id`) se vuelven `Screen`s
  selladas de **Voyager**, con el mismo redirect reactivo por rol que tenía
  el `redirect:` de GoRouter. Los `ShellRoute` (layouts con tabs) se
  implementan como Navigators anidados dentro de cada `Screen` de shell, en
  el módulo de features.
- `di/SharedModule.kt`: reemplaza el registro global de `Provider`s de
  Riverpod con un módulo de **Koin**. Se inyecta una sola vez en el
  `androidApp`/`iosApp` y todo lo que migremos después (services, otros
  providers) se agrega a este mismo módulo.

## Módulo 3: services (offline, sync, ubicación, fotos, PDF, dashboard) — agregado

- **`db/` (SQLDelight)**: reemplaza `hive`/`hive_flutter`. Los 5 "boxes" de
  Hive (`offline_orders`, `offline_equipment`, `offline_quotes`,
  `sync_queue`, `settings`) se modelan en `Cooltrack.sq` como una tabla
  key-value (`CacheEntry`) + una tabla real para la cola de sync
  (`SyncQueueItem`), lo que permite borrar un solo ítem de la cola sin
  reescribir toda la lista (mejora sobre el original, que reescribía el
  array completo en cada `removeSyncItem`).
- **`services/OfflineRepository.kt`**: mismo contrato público que el
  original (`cacheOrders`, `getCachedOrders`, `addToSyncQueue`,
  `getSyncQueue`, `clearAllCache`, etc.), ahora sobre SQLDelight.
- **`services/LocationProvider.kt`** (+ `.android.kt` con
  FusedLocationProviderClient / `.ios.kt` con CLLocationManager): reemplaza
  `geolocator`. **`services/LocationService.kt`** (commonMain) orquesta el
  stream de posiciones con la inserción en `technician_locations` de
  Supabase — misma lógica que `location_service.dart`, con el mismo
  `distanceFilter` de 50 metros.
- **`services/ImagePickerService.kt`** (+ Android/iOS): reemplaza
  `image_picker`. ⚠️ A diferencia de Flutter, elegir imagen desde cámara o
  galería necesita un `Activity`/`UIViewController` presentador — algo que
  `shared` no tiene. Quedó documentado como contrato a conectar desde la capa
  de UI (`androidApp`/`iosApp`) en el próximo módulo de features; el
  post-procesado (resize + compresión JPEG, igual que `maxWidth/maxHeight/quality`)
  ya está implementado en Android.
- **`services/PhotoUploadService.kt`**: reemplaza la subida directa a
  Cloudinary con `dio` → ahora con Ktor multipart. Mismo endpoint, mismo
  `UploadResult`.
- **`services/SyncService.kt`**: misma máquina de "cola de sincronización"
  que el original (`insert`/`update`/`delete`/`upload_media`/`upload_signature`),
  ahora con `SyncStatus` como `StateFlow` en vez de listeners manuales.
  Diferencia de diseño: en vez de guardar una ruta de archivo local (`file_path`,
  que no es portable 1:1 entre Android/iOS/desktop), la cola guarda los bytes
  de la foto/firma en Base64 dentro del mismo registro — evita depender de
  que el archivo temporal siga existiendo en disco al momento de sincronizar.
- **`services/PdfContent.kt` + `services/PdfService.kt`** (+ Android con
  `PdfDocument`/`Canvas`, iOS con `UIGraphicsPDFRenderer`): reemplaza
  `pdf`/`printing`. Se separó el armado de contenido (formateo de fechas,
  moneda, filas de la tabla — 100% compartido) del motor de dibujo
  (`expect`/`actual`, porque Android y iOS no comparten una librería de
  layout de PDF como sí comparten Compose para UI). El render de Android
  reproduce fielmente el layout original (header con marca, tabla con
  bordes, totales); el de iOS es una versión funcional más simple (bloques
  de texto) que se puede refinar visualmente sin tocar el resto del código.
- **`services/DashboardService.kt`**: conversión directa 1:1 de
  `dashboard_service.dart`.
- **`di/SharedModule.kt`**: se agregó el registro de `SupabaseClient`
  (`createSupabaseClient` con los plugins `GoTrue`, `Postgrest`, `Realtime`,
  `Storage` — equivalente a `Supabase.instance.client` de `supabase_flutter`)
  y de todos los servicios nuevos, todos inyectables por constructor.

## Módulo 4: providers restantes + arranque del módulo de features — agregado

- **`notifications/NotificationRepository.kt`**: reemplaza `notification_provider.dart`.
  El `StreamProvider` de Riverpod sobre Supabase Realtime se arma con
  `supabase.realtime.channel(...).postgresChangeFlow(...)`, expuesto como
  `StateFlow<List<AppNotification>>` + `unreadCount` derivado, y las mismas
  operaciones (`markAsRead`, `sendNotification`) como métodos de la clase.
- **`location/LocationRepository.kt`**: reemplaza `location_provider.dart`
  (la capa de estado de UI sobre `services/LocationService`). Mismo
  `LocationStatus`/`LocationState` (acá `LocationUiState` para no chocar con
  `AuthState`), mismos métodos (`init`, `getCurrentLocation`, `startTracking`,
  `stopTracking`, `isWithinRadius` — implementado con fórmula de Haversine).
- **`photo/PhotoUploadRepository.kt`**: reemplaza `photo_upload_provider.dart`,
  incluyendo el patrón `.family` de Riverpod (una instancia distinta por
  `orderId`/`equipmentId`/`context`). En Koin esto se resuelve con un
  `factory { (orderId, equipmentId, ctx) -> ... }` + `parametersOf(...)` al
  inyectar, en vez de una key implícita como en Riverpod.
- **`core/AppTheme.kt`**: reemplaza `core/theme.dart`. Mismos `AppColors`
  (primary, secondary, estados de orden, etc.) y un `CooltrackTheme(...)`
  con `MaterialTheme` de Compose Multiplatform equivalente a `AppTheme.light`.
- **`util/ComposeFlow.kt`**: helper único `StateFlow<T>.collectAsStateSimple()`
  reutilizado en todas las pantallas — es el mismo rol que `ref.watch(provider)`
  tenía en cada widget de Riverpod, pero sin depender de un framework de
  estado específico de Compose (no requiere `lifecycle-viewmodel-compose`).
- **`features/auth/LoginScreen.kt`**: primera pantalla real migrada
  (`login_screen.dart` → Voyager `Screen` + Compose), formulario completo,
  validación, snackbar de error, y el mismo redirect post-login por rol.
- **`features/admin/AdminShellScreen.kt`, `features/tech/TechnicianShellScreen.kt`,
  `features/client/ClientShellScreen.kt`**: placeholders navegables (con
  logout funcional) para los tres `ShellRoute` de go_router. Las pantallas
  internas de cada rol (dashboard, listado de órdenes, gestión de clientes,
  etc.) quedan para el próximo módulo — son ~15 archivos de vistas por rol.
- **`navigation/AppNavigation.kt`**: ajustado para navegar directamente entre
  las `Screen`s reales (`LoginScreen`, `AdminShellScreen`, etc.) en vez de un
  enum intermedio.
- **`androidApp/`** (nuevo módulo Gradle): primer punto de entrada real.
  - `CooltrackApplication.kt`: equivalente al arranque de `main.dart`
    (`WidgetsFlutterBinding` + `Supabase.initialize` + `OfflineRepository().init()`)
    — acá arranca Koin y `SecureStorageInitializer`. Las claves de Supabase/
    Cloudinary se leen de `local.properties` (no versionado) en vez de un
    `.env.local` empaquetado.
  - `MainActivity.kt`: equivalente a `runApp(ProviderScope(child: MyApp()))`
    — monta `CooltrackApp(authRepository)` con Compose y dispara
    `authRepository.init()`.
  - `AndroidManifest.xml` con los permisos usados por los servicios ya
    migrados (`INTERNET`, ubicación, cámara) y el `FileProvider` que usa
    `PdfRenderer.previewOrShare`.

## Módulo 5a: librería de componentes UI compartidos (agregado)

Primera sub-parte del módulo 5. Antes de migrar las ~17 vistas de admin (y
luego técnico/cliente), se migró `lib/components/*.dart` completo a
`shared/src/commonMain/.../ui/components/`, porque **todas** las pantallas
de los tres roles dependen de estos widgets:

- **`AppIcons.kt`**: mapea cada constante de `AppIcons` (Flutter `IconData`)
  a su `ImageVector` más cercano en `material-icons-extended` (ya declarado
  en `shared/build.gradle.kts`), distinguiendo variante *outlined*/*filled*
  igual que el original.
- **`AppButton.kt`**: `AppButton` (4 variantes, loading, full width, ícono)
  + `AppIconButton`. `tooltip` queda documentado pero sin efecto (Compose
  Multiplatform no tiene `Tooltip` unificado Android/iOS aún).
- **`AppCard.kt`**: `AppCard`, `AppCardSkeleton`, `AppListTile`.
- **`AppInput.kt`**: `AppInput` + `AppSearchInput`. Cambio de diseño
  importante: el `TextEditingController` imperativo de Dart se reemplaza
  por *state hoisting* (`value` + `onValueChange`), que es el patrón
  idiomático de Compose — cada pantalla que antes tenía un
  `TextEditingController` por campo ahora va a tener un `var` (o un campo
  de su `ScreenModel`) en su lugar.
- **`AppModal.kt`**: `AppModal` (ahora `ModalBottomSheet` de Material3) +
  `AppConfirmDialog` (`AlertDialog`). Los `static Future<T?> show(...)`
  imperativos de Dart se reemplazan por el mismo patrón reactivo que ya usa
  el resto del proyecto: la pantalla mantiene `var showX by remember { ... }`
  y renderiza el composable condicionalmente.
- **`AppToast.kt`**: como Compose no tiene un `ScaffoldMessenger.of(context)`
  global, se agregó `AppToastState`/`rememberAppToastState()` +
  `AppToastHost` — cada `Scaffold` de pantalla crea su propio estado con
  `rememberAppToastState()`, lo conecta con
  `Scaffold(snackbarHost = { AppToastHost(toastState) })`, y dispara
  `toastState.showSuccess(...)` / `.showError(...)` etc. igual que antes.
  `AppLoadingOverlay` se simplificó a un solo wrapper reactivo
  (`isLoading: Boolean`), eliminando el `show(context)`/`hide(context)`
  imperativo del original (mismo criterio que ya se usó para `AuthState`).
- **`AppStatusBadge.kt`**: `AppStatusBadge`/`AppQuoteStatusBadge`, ahora
  usando `OrderStatus.label`/`QuoteStatus.label` de `Constants.kt` en vez
  de los mapas `orderStatusLabels`/`quoteStatusLabels` aparte.
- **`AppListItem.kt`**: `AppListItem`, `AppSectionHeader`, `AppEmptyState`.
- **`AppAvatar.kt`**: `AppAvatar`/`AppAvatarGroup`. Se agregó **Coil3**
  (`coil-compose` + `coil-network-ktor3`) como dependencia nueva en
  `shared/build.gradle.kts` — es el reemplazo multiplatform de
  `Image.network(...)`, apoyado en el mismo Ktor que ya usa `ApiClient`.
- **`SignaturePad.kt`** (+ `.android.kt` con `Bitmap`/`Canvas`/`Path`, `.ios.kt`
  con `UIGraphicsImageRenderer`/`UIBezierPath`): reemplaza el paquete
  `signature` de Flutter. La captura del trazo se dibuja a mano con
  `Canvas`+`Path` de Compose (según lo ya anotado en el módulo 3). Cambio de
  diseño: en vez de `onSave(filePath: String)` escribiendo a un directorio
  temporal, `onSave(bytes: ByteArray)` entrega el PNG ya codificado — mismo
  criterio "bytes en vez de rutas de archivo" que se usó para
  `SyncService` en el módulo 3.
- **`SyncIndicator.kt`**: reemplaza el `addListener`/`dispose` manual del
  original por `SyncService.status` (`StateFlow<SyncStatus>`) observado con
  `collectAsStateSimple()`.

## Próximos módulos (en orden sugerido)

5. **Pantallas internas por rol** — dashboard/clientes/equipos/cotizaciones
   (admin), lista y detalle de orden con evidencias/firma (técnico),
   solicitud y seguimiento de servicio (cliente).
   - **5a. Librería de componentes UI compartidos** — ✅ hecho (ver arriba).
   - **5b. Admin** — providers + 17 vistas de `features/admin/views/*.dart`.
     - **Parte 1 (dashboard, clientes, equipos, técnicos)** — ✅ hecho:
       `AdminRepository.kt` (Supabase-directo, de `admin_provider.dart`),
       `ApiClientExtensions.kt` (helpers `getListData`/`getObjectDataOrNull`
       para las pantallas que van por el backend REST, que son la mayoría),
       `AdminShellScreen.kt` (shell real con navegación inferior de 5 tabs,
       reemplaza el placeholder del módulo 4), `AdminDashboardScreen.kt`,
       `AdminClientsScreen.kt` / `AdminClientDetailScreen.kt` /
       `AdminClientNewScreen.kt`, `AdminEquipmentScreen.kt` /
       `AdminEquipmentDetailScreen.kt` / `AdminEquipmentNewScreen.kt`,
       `AdminTechniciansScreen.kt` / `AdminCreateTechnicianScreen.kt`.
       Placeholders temporales agregados para que la navegación no quede
       rota mientras tanto: `AdminOrdersScreen.kt`, `AdminQuotesScreen.kt`
       (se completan en la parte 2) y `NotificationsScreen.kt` (se completa
       en el módulo 5e).
       Fix menor respecto al original: en `admin_technicians_screen.dart`
       el FAB de "+" no estaba conectado a ninguna acción (`// Add
       technician` comentado); acá se conectó a `AdminCreateTechnicianScreen`,
       que ya existe como pantalla y ruta en el proyecto original.
     - **Parte 2 (órdenes, cotizaciones, catálogo, reportes, tracking)** —
       ✅ hecho: `AdminOrdersScreen.kt` / `AdminOrderDetailScreen.kt`
       (asignación de técnico vía bottom sheet, cambios de estado
       encolados con `SyncService`, exportación a PDF con
       `PdfContentBuilder`/`PdfService` ya migrados en el módulo 3),
       `AdminQuotesScreen.kt` / `AdminQuoteNewScreen.kt` (ítems dinámicos,
       cálculo de subtotal/IVA/total, insert directo a Supabase igual que
       el original), `AdminServiceCatalogScreen.kt`,
       `ReportsRepository.kt` (de `reports_provider.dart`, con el mismo
       join embebido de Postgrest que usaba el original) +
       `AdminReportsScreen.kt`, `AdminTechTrackingScreen.kt`.

       Dos decisiones de diseño para dejar anotadas:
       - **Gráficos (`AdminReportsScreen`)**: el original usa `fl_chart`
         (`PieChart`/`BarChart`), sin equivalente directo en Compose
         Multiplatform. Se dibujaron a mano con `Canvas` (mismo criterio
         que `SignaturePad` del módulo 5a) — sin agregar ninguna librería
         de gráficos nueva.
       - **Mapa de rastreo (`AdminTechTrackingScreen`)**: el original usa
         `google_maps_flutter` con un `GoogleMap` real. Este proyecto
         todavía no tiene un SDK de mapas agregado a `build.gradle.kts`
         (agregar Google Maps Compose para Android + un puente nativo para
         iOS es un trabajo de integración del tamaño de
         `ImagePickerService`/`PdfRenderer`, no algo para resolver de
         paso). Se preservó la parte que sí importa — la suscripción en
         tiempo real a `technician_locations` de Supabase Realtime — y se
         muestra como lista (nombre, coordenadas, hora de última
         actualización) en vez de marcadores sobre un mapa. Cambiar esto
         por un mapa real después no toca la suscripción, solo la capa de
         presentación.

       Puntos de mayor incertidumbre de API en esta parte (sin poder
       compilar en este entorno para verificar contra la versión exacta
       de `postgrest-kt` resuelta por Gradle — quedan comentados en el
       código en el lugar exacto):
       - `Columns.raw(...)` en `ReportsRepository.getReports()` (columna
         con join embebido `users!service_orders_technician_id_fkey(name)`).
       - `.insert(value) { select(...) }.decodeSingle<T>()` en
         `AdminQuoteNewScreen.createQuote()` (recuperar el `id` generado
         al insertar la cotización).

     Con esto queda completo el módulo 5b (admin) — 22 vistas/providers en
     total entre las dos partes.
   - **5c. Técnico** — providers (`tech_jobs_provider.dart`,
     `parts_provider.dart`) + componentes propios de técnico
     (`photo_capture_component.dart`, `signature_component.dart`,
     `parts_selector_component.dart`) + vistas de
     `features/tech/views/*.dart`. Pendiente.
   - **5d. Cliente** — `client_provider.dart` + vistas de
     `features/client/views/*.dart`. Pendiente.
   - **5e. Notificaciones** — `notifications_screen.dart` (usa
     `NotificationRepository` del módulo 4). Pendiente.
6. **`iosApp`** — proyecto Xcode + `App.swift` que monta el `Shared.framework`
   generado por `shared`, análogo a `androidApp/MainActivity.kt`. Incluye
   conectar los puentes pendientes: `ImagePickerService` (PHPicker) y el
   `UIActivityViewController` de `PdfRenderer.previewOrShare`.
7. **Notificaciones push** — `firebase_messaging` → Firebase Kotlin SDK o
   servicios nativos (FCM en Android, APNs en iOS) + `flutter_local_notifications`
   → `NotificationManager` nativo / `UNUserNotificationCenter`.

Decime cuándo seguimos con el módulo 5 (pantallas internas por rol) y continuamos.
