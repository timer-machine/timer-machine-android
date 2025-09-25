plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.timer.one"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))
    implementation(project(":component-settings"))

    implementation(libs.androidx.dynamicAnimation)

    implementation(libs.fastAdapter)

    implementation(libs.chromeMenu)
    implementation(libs.flexbox)
    implementation(libs.materialPopupMenu)

}
