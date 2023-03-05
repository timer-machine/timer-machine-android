package io.github.deweyreed.timer

import dagger.Reusable
import xyz.aprildown.timer.app.base.data.FlavorData
import javax.inject.Inject

@Reusable
class FlavorDataImpl @Inject constructor() : FlavorData {
    override val flavor: FlavorData.Flavor = FlavorData.Flavor.Google
    override val appDownloadLink: String =
        "https://play.google.com/store/apps/details?id=io.github.deweyreed.timer.google"
}
