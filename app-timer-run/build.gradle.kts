plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.timer.run"
}

dependencies {
    implementation(project(":data"))
    implementation(project(":app-base"))
    implementation(project(":component-tts"))

    implementation(libs.androidx.media)

}
