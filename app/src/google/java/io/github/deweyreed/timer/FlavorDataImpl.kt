package io.github.deweyreed.timer

import dagger.Reusable
import xyz.aprildown.timer.app.base.data.FlavorData
import javax.inject.Inject

@Reusable
class FlavorDataImpl @Inject constructor() : FlavorData {
    override val supportAdvancedFeatures: Boolean = true
}
