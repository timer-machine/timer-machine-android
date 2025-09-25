plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.scheduler"
}

dependencies {
    implementation(project(":app-base"))

    implementation(libs.materialPopupMenu)
    implementation(libs.toggleButtonGroup)

}
