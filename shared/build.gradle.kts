import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.sqldelight")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += listOf(
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                    "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
                )
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // --- Equivalentes a las dependencias de pubspec.yaml ---

                // flutter_riverpod -> ViewModel/StateFlow propio (ver providers/)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // json_annotation / freezed -> kotlinx.serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // dio -> Ktor client
                implementation("io.ktor:ktor-client-core:3.0.1")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
                implementation("io.ktor:ktor-client-logging:3.0.1")

                // supabase_flutter -> supabase-kt
                implementation("io.github.jan-tennert.supabase:postgrest-kt:3.0.2")
                implementation("io.github.jan-tennert.supabase:auth-kt:3.0.2")
                implementation("io.github.jan-tennert.supabase:realtime-kt:3.0.2")
                implementation("io.github.jan-tennert.supabase:storage-kt:3.0.2")

                // hive -> SQLDelight (persistencia offline)
                implementation("app.cash.sqldelight:runtime:2.0.2")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

                // flutter_secure_storage -> multiplatform-settings (con backend seguro por plataforma)
                implementation("com.russhwolf:multiplatform-settings:1.2.0")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.2.0")

                // Compose Multiplatform (equivalente al framework de Widgets de Flutter)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                // Image.network (Flutter) -> Coil3 (carga de imágenes remotas
                // multiplatform, usada por AppAvatar y por las miniaturas de
                // evidencia fotográfica en las pantallas de técnico/admin).
                // Se apoya en el mismo motor Ktor ya declarado arriba.
                implementation("io.coil-kt.coil3:coil-compose:3.0.4")
                implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.4")

                // go_router -> Voyager (navegación multiplatform)
                implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03")
                implementation("cafe.adriel.voyager:voyager-screenmodel:1.1.0-beta03")
                implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta03")

                // Riverpod (Provider/ref.watch) -> Koin (inyección de dependencias KMP)
                implementation("io.insert-koin:koin-core:4.0.0")
                implementation("io.insert-koin:koin-compose:4.0.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.0.1")
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                // geolocator -> Google Play Services Location
                implementation("com.google.android.gms:play-services-location:21.3.0")
                // EncryptedSharedPreferences
                implementation("androidx.security:security-crypto:1.1.0")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.1")
                implementation("app.cash.sqldelight:native-driver:2.0.2")
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
    }
}

android {
    namespace = "com.datasys.cooltrack.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24

        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) load(f.inputStream())
        }
        fun secret(key: String) = localProps.getProperty(key) ?: System.getenv(key) ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${secret("CLOUDINARY_API_KEY")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${secret("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${secret("CLOUDINARY_UPLOAD_PRESET")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("CooltrackDatabase") {
            packageName.set("com.datasys.cooltrack.db")
        }
    }
}
