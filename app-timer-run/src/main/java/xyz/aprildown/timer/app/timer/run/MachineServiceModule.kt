package xyz.aprildown.timer.app.timer.run

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import xyz.aprildown.timer.presentation.stream.MachineContract
import xyz.aprildown.timer.presentation.stream.MachinePresenter

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MachineServiceModule {
    @Binds
    abstract fun provideMachineServicePresenter(presenter: MachinePresenter): MachineContract.Presenter
}
