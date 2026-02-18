plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.broadcast"
}

dependencies {
    implementation(project(":app-base"))
}
