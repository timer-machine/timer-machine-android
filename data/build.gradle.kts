plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "xyz.aprildown.timer.data"

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.bundles.androidx.test.espresso)
    androidTestImplementation(libs.androidx.room.test)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.moshi.core)
    ksp(libs.moshi.kotlinGen)

    implementation(libs.androidJob)

}
