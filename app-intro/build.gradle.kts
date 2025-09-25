plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.intro"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))
    implementation(project(":app-timer-list"))
    implementation(project(":app-timer-edit"))
    implementation(project(":app-timer-one"))

    implementation(libs.flexbox)
    implementation(libs.konfetti)

}
