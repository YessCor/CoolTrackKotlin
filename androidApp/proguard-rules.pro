# ============================================================================
#  CoolTrack — reglas R8 / ProGuard para el build release
#  Objetivo: ofuscar y reducir TODO lo posible (incluida la estructura de
#  clases propias) sin romper serialización, DI (Koin), navegación (Voyager)
#  ni SQLDelight.
#
#  Nota: kotlinx.serialization, Ktor, Coil, SQLDelight, Compose y AndroidX
#  ya traen sus propias reglas de consumidor. Acá solo agregamos lo que un
#  proyecto Kotlin Multiplatform necesita de más, y afinamos qué se conserva.
# ============================================================================

-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keepattributes *Annotation*,Exceptions
# El nombre de archivo/línea originales NO se conservan (obliga a un
# retrace con el mapping para leer stacktraces). El mapping queda en
# androidApp/build/outputs/mapping/release/ — guardalo por versión.
-renamesourcefileattribute SourceFile

# --- Borrar logs en release -------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static boolean isLoggable(java.lang.String, int);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ----------------------------------------------------------------------------
#  kotlinx.serialization  (se permite ofuscar los nombres de clase; lo que
#  importa para el JSON son los @SerialName, que son explícitos en el modelo)
# ----------------------------------------------------------------------------
-keepclassmembers class **$$serializer {
    *** descriptor;
    kotlinx.serialization.KSerializer[] childSerializers();
    kotlinx.serialization.KSerializer[] typeParametersSerializers();
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** { public static ** INSTANCE; }
-keepclassmembers class <1> {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Los constructores sintéticos que usa el deserializador.
-keepclassmembers class * {
    public synthetic <init>(int, ***, kotlinx.serialization.internal.SerializationConstructorMarker);
}
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Enums usados por valueOf()/serialización: se puede ofuscar la clase, pero
# NO los nombres de las constantes (AppLogoMark.valueOf(...), @SerialName).
-keepclassmembers,allowobfuscation enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

-keep class kotlinx.datetime.serializers.** { *; }
-dontwarn kotlinx.datetime.**

# ----------------------------------------------------------------------------
#  Ktor / coroutines / OkHttp / supabase-kt  (solo silenciar + reflexión mínima)
# ----------------------------------------------------------------------------
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn io.github.jan.supabase.**
-keepclassmembernames class io.ktor.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlin.reflect.jvm.internal.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# ----------------------------------------------------------------------------
#  Koin
# ----------------------------------------------------------------------------
-dontwarn org.koin.**

# ----------------------------------------------------------------------------
#  Voyager — se compara/instancia por TIPO. Se permite ofuscar el nombre de
#  cada Screen, pero no se puede borrar/mergear la clase.
# ----------------------------------------------------------------------------
-keep,allowobfuscation class * implements cafe.adriel.voyager.core.screen.Screen
-keepnames class * implements cafe.adriel.voyager.core.screen.Screen
-dontwarn cafe.adriel.voyager.**

# ----------------------------------------------------------------------------
#  SQLDelight — código generado. Se permite ofuscar; solo evitar que R8
#  rompa la reflexión interna del runtime.
# ----------------------------------------------------------------------------
-keep class app.cash.sqldelight.** { *; }
-dontwarn app.cash.sqldelight.**

# ----------------------------------------------------------------------------
#  BuildConfig (se lee en runtime desde AppConfig.android.kt)
# ----------------------------------------------------------------------------
-keepclassmembers class com.datasys.cooltrack.shared.BuildConfig { public static <fields>; }
-keepclassmembers class com.datasys.cooltrack.android.BuildConfig { public static <fields>; }

-dontwarn androidx.compose.**
-dontwarn coil3.**
-dontwarn com.google.android.gms.**
