plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.android.libraryCompose)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.base"
}

dependencies {
    api(project(":presentation"))

    testImplementation(libs.junit)

    api(libs.androidx.recyclerView)
    implementation(libs.androidx.preference)
    api(libs.androidx.constraintLayout)
    implementation(libs.androidx.customTabs)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.player)
    api(libs.androidx.fragment)

    api(libs.material)

    api(libs.androidx.navigation.fragment)
    api(libs.androidx.navigation.ui)

    implementation(libs.materialColorUtilities)

    implementation(libs.fastAdapter.core)

    implementation(libs.materialPopupMenu)
    api(libs.materialize)
    implementation(libs.theme)

}
