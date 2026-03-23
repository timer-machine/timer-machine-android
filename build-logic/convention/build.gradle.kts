plugins {
    `kotlin-dsl`
}

group = "io.github.deweyreed.timer.buildlogic"

java {
    val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget
            .fromTarget(libs.versions.jvmTarget.get())
    }
}

dependencies {
    compileOnly(plugin(libs.plugins.android.application))
    compileOnly(plugin(libs.plugins.android.library))
    compileOnly(plugin(libs.plugins.kotlin.compose))
    compileOnly(plugin(libs.plugins.kotlin.kapt))
    compileOnly(plugin(libs.plugins.hilt))
    compileOnly(plugin(libs.plugins.detekt))
}

private fun plugin(plugin: Provider<PluginDependency>): Provider<String> {
    return plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.convention.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.convention.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = libs.plugins.convention.android.libraryCompose.get().pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.convention.hilt.get().pluginId
            implementationClass = "HiltConventionPlugin"
        }
    }
}
