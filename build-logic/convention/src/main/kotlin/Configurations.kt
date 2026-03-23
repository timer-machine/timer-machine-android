import com.android.build.api.dsl.CommonExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureKotlin() {
    val libs = libs
    extensions.getByType<KotlinAndroidProjectExtension>().run {
        compilerOptions {
            jvmTarget.set(
                JvmTarget.fromTarget(libs.findVersion("jvmTarget").get().toString())
            )
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}

internal fun Project.configureAndroid(commonExtension: CommonExtension) {
    val libs = libs
    commonExtension.run {
        compileSdk = libs.findVersion("compileSdk").get().toString().toInt()
        defaultConfig.run {
            minSdk = libs.findVersion("minSdk").get().toString().toInt()

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true
        }
        compileOptions.run {
            isCoreLibraryDesugaringEnabled = true
            val javaVersion =
                JavaVersion.toVersion(libs.findVersion("jvmTarget").get().toString())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
        buildFeatures.run {
            viewBinding = true
        }
        dependencies {
            "coreLibraryDesugaring"(libs.findLibrary("desugarJdkLibs").get())
        }
    }
}

internal fun Project.configureCompose(commonExtension: CommonExtension) {
    val libs = libs
    commonExtension.buildFeatures.compose = true
    extensions.getByType<ComposeCompilerGradlePluginExtension>().run {
        stabilityConfigurationFiles.addAll(
            isolated.rootProject.projectDirectory.file("stability-config.conf"),
        )
    }
    dependencies {
        "implementation"(platform(libs.findLibrary("compose-bom").get()))
        "implementation"(libs.findBundle("compose").get())
    }
}

internal fun Project.configureDetekt() {
    val libs = libs
    apply(plugin = libs.findPlugin("detekt").get().get().pluginId)

    extensions.getByType<DetektExtension>().run {
        toolVersion = libs.findVersion("detekt").get().toString()
        config.setFrom(
            isolated.rootProject.projectDirectory
                .file("detekt-config.yml").asFile.canonicalPath
        )
        buildUponDefaultConfig = true
    }
    tasks.withType<Detekt>().configureEach {
        reports {
            xml.required.set(false)
            txt.required.set(false)
        }
    }
    dependencies {
        "detektPlugins"(libs.findLibrary("detekt-formatting").get())
    }
}
