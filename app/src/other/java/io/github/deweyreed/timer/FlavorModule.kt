package io.github.deweyreed.timer

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.app.base.data.FlavorData

@Module
@InstallIn(SingletonComponent::class)
abstract class FlavorModule {
    @Binds
    abstract fun bindFlavorData(impl: FlavorDataImpl): FlavorData
}
