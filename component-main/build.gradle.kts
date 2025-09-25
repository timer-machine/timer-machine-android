plugins {
    alias(libs.plugins.convention.android.library)
}

android {
    namespace = "xyz.aprildown.timer.workshop"
}

dependencies {
    implementation(project(":app-base"))

    implementation(libs.androidx.preference)

    implementation(libs.fastAdapter)
    implementation(libs.fastAdapter.binding)

}
