import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val libs = libs

            apply(plugin = libs.findPlugin("android-library").get().get().pluginId)
            apply(plugin = libs.findPlugin("kotlin-compose").get().get().pluginId)

            extensions.getByType<LibraryExtension>().run {
                configureCompose(this)
            }
        }
    }
}
