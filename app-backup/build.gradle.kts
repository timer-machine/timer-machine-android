plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.android.libraryCompose)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.backup"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.bundles.mockito.core)

    implementation(libs.androidx.preference)
    implementation(libs.androidx.documentFile)

    implementation(libs.okio)
    implementation(libs.permission)

}
