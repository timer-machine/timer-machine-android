plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.settings"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))
    implementation(project(":component-settings"))
    implementation(project(":component-tts"))

    implementation(libs.androidx.preference)

    implementation(libs.fastAdapter)

    implementation(libs.materialDialog.core)
    implementation(libs.materialDialog.common)
    implementation(libs.permission)

}
