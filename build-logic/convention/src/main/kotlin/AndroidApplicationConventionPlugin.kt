import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.run {
            val libs = libs

            apply(plugin = libs.findPlugin("android-application").get().get().pluginId)
            apply(plugin = libs.findPlugin("kotlin-android").get().get().pluginId)
            apply(plugin = libs.findPlugin("kotlin-compose").get().get().pluginId)

            configureKotlin()

            val versionCode = libs.findVersion("versionCode").get().toString().toInt()
            val versionName = libs.findVersion("versionName").get().toString()
            extensions.getByType<ApplicationExtension>().run {
                configureAndroid(this)
                defaultConfig {
                    targetSdk = libs.findVersion("targetSdk").get().toString().toInt()
                    this.versionCode = versionCode
                    this.versionName = versionName
                }
                configureCompose(this)
            }
            extensions.getByType<BasePluginExtension>().run {
                archivesName.set("TimeR.Machine-v${versionName}(${versionCode})")
            }

            configureDetekt()
        }
    }
}
