plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.github.deweyreed.timer.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    defaultConfig {
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    flavorDimensions.add("market")
    productFlavors {
        create("dog") { dimension = "market" }
        create("google") { dimension = "market" }
        create("other") { dimension = "market" }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget
            .fromTarget(libs.versions.jvmTarget.get())
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.benchmarkMacroJunit4)
}

androidComponents {
    onVariants(selector().all()) { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
                "targetAppId",
            v.testedApks.map { checkNotNull(artifactsLoader.load(it)).applicationId }
        )
    }
}
