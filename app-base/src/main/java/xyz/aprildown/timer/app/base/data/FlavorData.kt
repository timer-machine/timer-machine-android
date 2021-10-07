package xyz.aprildown.timer.app.base.data

interface FlavorData {
    val email: String get() = "ligrsidfd@gmail.com"
    val appDownloadLink: String
        get() = "https://play.google.com/store/apps/details?id=io.github.deweyreed.timer.google"
    val supportAdvancedFeatures: Boolean get() = false
}
