import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.KaptExtension

@Suppress("unused")
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val libs = libs

            apply(plugin = libs.findPlugin("kotlin-kapt").get().get().pluginId)
            apply(plugin = libs.findPlugin("hilt").get().get().pluginId)

            extensions.getByType<KaptExtension>().run {
                correctErrorTypes = true
            }

            dependencies {
                "implementation"(libs.findLibrary("hilt-android").get())
                "kapt"(libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
