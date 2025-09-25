plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.github.deweyreed.timer.component.tts"
}

dependencies {
    implementation(project(":app-base"))

    implementation(libs.androidx.work)

    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.diskLruCache)
}
