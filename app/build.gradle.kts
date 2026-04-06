import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.convention.android.application)
    alias(libs.plugins.convention.hilt)

    alias(libs.plugins.dependencyGuard)
    // alias(libs.plugins.androidx.baselineprofile)

    alias(libs.plugins.gms)
    alias(libs.plugins.firebase.crashlytics)

    alias(libs.plugins.aboutLibraries)
}

val keystoreProperties = Properties()
val keystorePropertiesFile: File = rootProject.file("local.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use {
        keystoreProperties.load(it)
    }
}

android {
    namespace = "io.github.deweyreed.timer"
    defaultConfig {
        applicationId = "io.github.deweyreed.timer"
        androidResources.localeFilters += setOf(
            "en",
            "de",
            "es",
            "fr",
            "nl",
            "pt",
            "ru",
            "ta",
            "zh-rCN",
            "zh-rHK",
            "zh-rTW",
        )
    }

    val signingConfigName = "signingConfig"
    signingConfigs {
        if (keystoreProperties["storeFile"] != null) {
            create(signingConfigName) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName(signingConfigName)?.let {
                signingConfig = it
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "market"
    productFlavors {
        create("dog") {
            isDefault = true
            dimension = "market"
            versionNameSuffix = "-dog"
            applicationIdSuffix = ".dog"
        }
        create("google") {
            dimension = "market"
            applicationIdSuffix = ".google"
        }
        create("other") {
            dimension = "market"
            applicationIdSuffix = ".other"
        }
    }
    dependenciesInfo {
        includeInApk = false
    }

    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/library_release.kotlin_module",
                "META-INF/library-core_release.kotlin_module",
            )
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":presentation"))
    implementation(project(":app-base"))
    implementation(project(":app-scheduler"))
    implementation(project(":app-backup"))
    implementation(project(":app-settings"))
    implementation(project(":app-tasker"))
    implementation(project(":app-intro"))
    implementation(project(":app-timer-edit"))
    implementation(project(":app-timer-run"))
    implementation(project(":app-timer-list"))
    implementation(project(":app-timer-one"))
    implementation(project(":component-key"))
    implementation(project(":component-main"))
    implementation(project(":component-settings"))
    implementation(project(":component-tts"))
    "googleImplementation"(project(":flavor-google"))
    "dogImplementation"(project(":app-analytics-fake"))
    "otherImplementation"(project(":app-analytics-fake"))

    // baselineProfile(project(":baselineprofile"))

    implementation(libs.tools)

    implementation(libs.kotlin.coroutines.android)

    androidTestImplementation(libs.bundles.mockito.android)
    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.bundles.androidx.test.espresso)
    androidTestImplementation(libs.hmsPicker)
    androidTestImplementation(libs.scrollHmsPicker)
    androidTestImplementation(libs.hilt.testing)
    kaptAndroidTest(libs.hilt.compiler)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.viewPager2)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.liveData)
    implementation(libs.androidx.lifecycle.viewModel)
    implementation(libs.androidx.profileinstaller)

    implementation(libs.material)

    debugImplementation(libs.leakCannary)

    implementation(libs.androidx.room.runtime)

    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation(libs.androidx.work)

    implementation(libs.hilt.work)

    implementation(libs.okio)
    implementation(libs.permission)

    implementation(libs.flexbox)
    implementation(libs.materialDrawer)
    implementation(libs.scrollHmsPicker)
    implementation(libs.theme)
    implementation(libs.toggleButtonGroup)
    implementation(libs.ultimateRingtonePicker)
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation(libs.aboutLibraries)

    androidTestCompileOnly(libs.taskerPlugin)
}

androidComponents.onVariants { variant ->
    if (variant.flavorName != "google") {
        val variantNameCapitalized = variant.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        project.tasks.named("process${variantNameCapitalized}GoogleServices").getOrNull()
            ?.enabled = false
        project.tasks.named("injectCrashlyticsMappingFileId${variantNameCapitalized}")
            .getOrNull()?.enabled = false
        project.tasks.named("injectCrashlyticsVersionControlInfo${variantNameCapitalized}")
            .getOrNull()?.enabled = false
        try {
            project.tasks.named("uploadCrashlyticsMappingFile${variantNameCapitalized}")
                .getOrNull()?.enabled = false
        } catch (_: UnknownTaskException) {
        }
    }
}

dependencyGuard {
    configuration("dogReleaseRuntimeClasspath") {
        modules = true
    }
    configuration("googleReleaseRuntimeClasspath") {
        modules = true
    }
    configuration("otherReleaseRuntimeClasspath") {
        modules = true
    }
}
