plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.tasker"
}

dependencies {
    implementation(project(":app-base"))

    implementation(libs.taskerPlugin)
}
