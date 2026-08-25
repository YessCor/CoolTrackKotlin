package com.datasys.cooltrack.di

import com.datasys.cooltrack.auth.AuthRepository
import com.datasys.cooltrack.core.AppConfig
import com.datasys.cooltrack.db.DatabaseDriverFactory
import com.datasys.cooltrack.features.admin.AdminRepository
import com.datasys.cooltrack.features.admin.ReportsRepository
import com.datasys.cooltrack.features.tech.TechRepository
import com.datasys.cooltrack.features.client.ClientRepository
import com.datasys.cooltrack.location.LocationRepository
import com.datasys.cooltrack.notifications.NotificationRepository
import com.datasys.cooltrack.photo.PhotoUploadRepository
import com.datasys.cooltrack.services.ImagePickerService
import com.datasys.cooltrack.services.LocationProvider
import com.datasys.cooltrack.services.LocationService
import com.datasys.cooltrack.services.OfflineRepository
import com.datasys.cooltrack.services.PdfRenderer
import com.datasys.cooltrack.services.PdfService
import com.datasys.cooltrack.services.PhotoUploadService
import com.datasys.cooltrack.services.SyncService
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import org.koin.dsl.module

/**
 * Equivalente al conjunto de `Provider`/`StateNotifierProvider` de Riverpod.
 * Con Koin se registran una vez acá y se inyectan con `koinInject()` /
 * constructor injection en vez de `ref.watch(authProvider)`.
 */
val sharedModule = module {
    single { AuthRepository(get()) }

    // Equivalente a Supabase.instance.client (supabase_flutter)
    single {
        createSupabaseClient(
            supabaseUrl = AppConfig.supabaseUrl,
            supabaseKey = AppConfig.supabaseAnonKey,
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }

    single<DatabaseDriverFactory> { DatabaseDriverFactory() }
    single { OfflineRepository(get()) }

    single { ImagePickerService() }
    single { PhotoUploadService(get()) }

    single { LocationProvider() }
    single { LocationService(get(), get()) }

    single { SyncService(get(), get(), get()) }

    single { PdfRenderer() }
    single { PdfService(get()) }

    // Equivalentes a los providers restantes de Riverpod (Módulo 4)
    single { NotificationRepository(get()) }
    single { LocationRepository(get(), get()) }

    // Módulo 5b: providers de la sección admin
    single { AdminRepository(get()) }
    single { ReportsRepository(get()) }

    // Módulos 5c y 5d: técnicos y clientes
    single { TechRepository(get()) }
    single { ClientRepository(get()) }

    // Equivalente a `photoUploadProvider.family`: una instancia nueva por
    // cada (orderId, equipmentId, context), pedida con parametersOf(...)
    factory { (orderId: String?, equipmentId: String?, ctx: String?) ->
        PhotoUploadRepository(get(), get(), orderId, equipmentId, ctx)
    }
}
