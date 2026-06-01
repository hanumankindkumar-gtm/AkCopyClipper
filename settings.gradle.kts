pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            library("core-ktx", "androidx.core:core-ktx:1.12.0")
            library("lifecycle-runtime-ktx", "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
            library("activity-compose", "androidx.activity:activity-compose:1.8.2")
            
            // Compose
            library("compose-bom", "androidx.compose:compose-bom:2023.10.01")
            library("compose-ui", "androidx.compose.ui:ui")
            library("compose-ui-graphics", "androidx.compose.ui:ui-graphics")
            library("compose-ui-tooling", "androidx.compose.ui:ui-tooling")
            library("compose-ui-tooling-preview", "androidx.compose.ui:ui-tooling-preview")
            library("compose-material3", "androidx.compose.material3:material3")
            
            // Room
            library("room-runtime", "androidx.room:room-runtime:2.6.1")
            library("room-ktx", "androidx.room:room-ktx:2.6.1")
            library("room-compiler", "androidx.room:room-compiler:2.6.1")
            
            // Plugins
            plugin("android-application", "com.android.application").version("8.2.2")
            plugin("kotlin-android", "org.jetbrains.kotlin.android").version("1.9.22")
            plugin("kotlin-kapt", "org.jetbrains.kotlin.kapt").version("1.9.22")
        }
    }
}

rootProject.name = "CopyClipper"
include(":app")