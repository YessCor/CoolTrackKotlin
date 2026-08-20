package com.datasys.cooltrack.android

import android.app.Application
import com.datasys.cooltrack.core.SecureStorageInitializer
import com.datasys.cooltrack.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Equivalente al bloque de arranque de lib/main.dart:
 *   WidgetsFlutterBinding.ensureInitialized();
 *   await dotenv.load(fileName: ".env.local");
 *   await Supabase.initialize(...);
 *   await OfflineRepository().init();
 *   runApp(...);
 *
 * En KMP, la config (.env) ya no se carga en runtime (ver AppConfig +
 * BuildConfig), Supabase se crea de forma perezosa al pedir el
 * `SupabaseClient` desde Koin, y SQLDelight abre la base la primera vez que
 * se usa `OfflineRepository`. Lo único que sí hace falta inicializar a mano
 * es el Context para SecureStorage/SQLDelight (Hive no lo pedía porque el
 * plugin ya tenía acceso a él).
 */
class CooltrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        SecureStorageInitializer.init(this)

        startKoin {
            androidContext(this@CooltrackApplication)
            modules(sharedModule)
        }
    }
}
