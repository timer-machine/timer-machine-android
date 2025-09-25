plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "xyz.aprildown.timer.flavor.google"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))

    implementation(libs.kotlin.coroutines.play)

    implementation(libs.androidx.preference)

    implementation(libs.androidx.work)

    implementation(libs.hilt.navigation)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.billing)

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.playServices.review)

    implementation(libs.fastAdapter)
    implementation(libs.fastAdapter.binding)

    implementation(libs.zip4j)

}
