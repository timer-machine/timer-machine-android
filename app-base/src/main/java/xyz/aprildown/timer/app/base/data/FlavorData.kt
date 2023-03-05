package xyz.aprildown.timer.app.base.data

interface FlavorData {

    enum class Flavor {
        Dog, Google, Other,
    }

    val flavor: Flavor

    val email: String get() = "ligrsidfd@gmail.com"
    val appDownloadLink: String
}
