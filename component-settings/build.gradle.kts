plugins {
    alias(libs.plugins.convention.android.library)
}

android {
    namespace = "xyz.aprildown.timer.component.settings"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))
}
