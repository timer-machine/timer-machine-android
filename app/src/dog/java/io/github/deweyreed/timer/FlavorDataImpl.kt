package io.github.deweyreed.timer

import dagger.Reusable
import xyz.aprildown.timer.app.base.data.FlavorData
import javax.inject.Inject

@Reusable
class FlavorDataImpl @Inject constructor() : FlavorData {
    override val flavor: FlavorData.Flavor = FlavorData.Flavor.Dog
    override val appDownloadLink: String =
        "https://github.com/timer-machine/timer-machine-android/releases"
}
