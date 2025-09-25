plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.presentation"
}

dependencies {
    api(project(":domain"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.bundles.mockito.core)
    testImplementation(libs.androidx.test.arch)

    androidTestImplementation(libs.kotlin.coroutines.test)
    androidTestImplementation(libs.bundles.mockito.android)
    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.bundles.androidx.test.espresso)

    api(libs.androidx.lifecycle.runtime)
    api(libs.androidx.lifecycle.liveData)
    api(libs.androidx.lifecycle.viewModel)

    implementation(libs.accurateCountDownTimer)
}
