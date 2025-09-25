plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.android.libraryCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "xyz.aprildown.timer.component.key"
}

dependencies {
    implementation(project(":app-base"))

    implementation(libs.androidx.preference)

    implementation(libs.flexbox)
    implementation(libs.hmsPicker)
    implementation(libs.materialPopupMenu)
    implementation(libs.scrollHmsPicker)
    implementation(libs.coil)
    implementation(libs.zoomable)

}
