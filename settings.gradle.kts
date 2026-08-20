pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Cooltrack"

include(":shared")
include(":androidApp")
// include(":iosApp")  // el módulo iOS se maneja desde Xcode, no participa del build de Gradle directamente
