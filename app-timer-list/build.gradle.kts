plugins {
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
}

android {
    namespace = "xyz.aprildown.timer.app.timer.list"
}

dependencies {
    implementation(project(":app-base"))
    implementation(project(":component-key"))

    implementation(libs.fastAdapter.core)
    implementation(libs.fastAdapter.binding)
    implementation(libs.fastAdapter.expandable)

    implementation(libs.calendarView)
    implementation(libs.chart)
    implementation(libs.materialPopupMenu)
    implementation(libs.permission)

}
