# Seguridad de CoolTrack

Resumen de qué protege la app, qué se endureció y **qué tenés que hacer vos**
antes de publicar el APK.

---

## 1. La verdad sobre "que no roben el código"

Un APK **siempre** se puede desensamblar. R8/ProGuard (ya activado en release)
ofusca nombres, borra código muerto y hace el análisis mucho más lento y
caro — pero no es cifrado. La protección real de tus datos **no está en el
APK**, está en el backend (Supabase RLS + la Edge Function `secure-db`).
Por eso el orden de prioridad es: **backend primero, APK después.**

---

## 2. Lo que ya se hizo en este repo

### Backend
- **`supabase/functions/secure-db/index.ts` reescrita.** Antes: cualquier
  usuario logueado (incluido un cliente que se registra solo) podía leer y
  escribir **cualquier fila** de casi todas las tablas, porque la función
  corría con `service_role` y solo comprobaba "¿hay sesión?". Ahora valida
  rol + propiedad de la fila + lista blanca de columnas por rol. **Hay que
  desplegarla** (paso 3).

### App / APK
- **Llave de Cloudinary eliminada.** `CLOUDINARY_API_KEY` estaba embebida en
  el `BuildConfig` (y hardcodeada en `AppConfig.ios.kt`, versionada en git) y
  **no se usaba** — la subida de fotos va con *unsigned upload preset*, que es
  lo único que el cliente necesita. Se quitó de `AppConfig`, del
  `build.gradle.kts` y de iOS. → **Rotala igual en Cloudinary** (paso 4).
- **R8 / ofuscación** activada en el build `release`
  (`isMinifyEnabled` + `isShrinkResources` + `proguard-rules.pro` + R8 full mode).
- **Logs de red apagados en release.** Ktor `Logging` y `supabase-kt`
  quedan en `LogLevel.NONE` salvo en debug — así no se filtran tokens ni
  headers a `logcat`.
- **`FLAG_SECURE` en release**: bloquea capturas de pantalla, grabación de
  pantalla y la vista previa en "apps recientes".
- **Manifest**: `allowBackup=false`, reglas de backup/transferencia que
  excluyen todo, `usesCleartextTraffic=false` + `network_security_config`
  (HTTPS obligatorio; hay un override solo-debug para el backend local).
- **`dependenciesInfo.includeInApk=false`**: no se incrusta el bloque
  firmado con la lista de librerías/versiones.
- La `SUPABASE_ANON_KEY` sigue en el APK **a propósito**: es pública por
  diseño. Su única defensa es RLS + `secure-db`.

---

## 3. Desplegar la nueva `secure-db` (obligatorio)

1. Supabase Dashboard → **Edge Functions** → `secure-db`.
2. Pegá el contenido de `supabase/functions/secure-db/index.ts` → **Deploy**.
3. Probá con las 3 cuentas (admin / técnico / cliente) los flujos:
   - Cliente: crear equipo, editar/borrar equipo propio, crear solicitud de
     servicio, calificar orden completada, aprobar/rechazar cotización,
     marcar notificación leída.
   - Técnico: cambiar estado de una orden asignada, guardar notas, subir
     evidencia/firma, reportar ubicación.
   - Admin: crear cliente/técnico, editar catálogo, crear cotización, asignar
     técnico, informes.
   - **Negativo**: con la cuenta de cliente, intentá `update` de una orden
     que no es tuya o `update` del catálogo → debe devolver 403.

Si algún flujo legítimo se rompe con 403, es que falta una columna en una de
las listas blancas (`CLIENT_ORDER_UPDATE`, `EQUIPMENT_FIELDS`, etc.) — ajustá
ese `Set` y volvé a desplegar.

---

## 4. Rotar la llave de Cloudinary

La API key `XMNOpC8RFvJPVCsefOmGAM5kUQU` quedó en el historial de git.
Cloudinary Console → **Settings → Security → Access Keys** → generá una nueva
y revocá la vieja. La app **no la necesita** (usa el upload preset), así que
rotarla no rompe nada.

Verificá además que el **upload preset** `CoolTrackPro` sea **Unsigned** y que
tenga límites (formato, tamaño máximo, carpeta fija) — Console → Settings →
Upload → Upload presets.

---

## 5. RLS de Supabase (revisar / aplicar)

La app lee **directo** (sin pasar por `secure-db`) de: `service_orders`,
`quotes`, `quote_items`, `equipment`, `service_catalog`,
`technician_locations`, `notifications`. Si RLS en esas tablas permite
`SELECT` amplio a `authenticated`, un cliente puede leer datos de todos los
demás con solo la anon key.

`supabase/RLS_policies.sql` tiene una plantilla de políticas por rol y
propiedad, más el `REVOKE` de escritura para forzar que el único camino de
escritura sea `secure-db`. Revisala contra tu esquema real y aplicala en
Supabase → SQL Editor.

---

## 6. Firma del APK release (pendiente)

Hoy el build `release` se firma con el keystore de **debug** (para poder
generarlo y probarlo). **No publiques así.** Para el keystore real:

```bash
keytool -genkeypair -v -keystore cooltrack-release.jks \
  -alias cooltrack -keyalg RSA -keysize 4096 -validity 10000
```

Guardá el `.jks` **fuera del repo** y agregá a `local.properties` (que ya
está en `.gitignore`):

```
RELEASE_STORE_FILE=C:/ruta/segura/cooltrack-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=cooltrack
RELEASE_KEY_PASSWORD=...
```

Y en `androidApp/build.gradle.kts`, reemplazá el
`signingConfig = signingConfigs.getByName("debug")` del bloque `release` por
un `signingConfigs { create("release") { ... } }` que lea esas propiedades.
(Decime cuando tengas el keystore y lo cableo.)

---

## 7. Recomendado para producción (no incluido)

- **Play Integrity API** — verifica que la petición viene de un APK genuino
  no modificado, en un dispositivo no comprometido. Es la defensa real
  contra APK reempaquetados / bots. Se valida en `secure-db`.
- **Supabase Auth**: activar rate limiting, deshabilitar signups si los
  clientes los crea el admin, exigir verificación de email.
- **Monitoreo**: alertas en Supabase por picos de requests a `secure-db`.
- Subir a Play como **AAB** (no APK) para que Google re-firme y aplique
  ofuscación de recursos adicional.
