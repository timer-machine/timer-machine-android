package io.github.deweyreed.timer.di

import android.content.Context
import android.content.Intent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.deweyreed.timer.ui.AppNavigatorImpl
import xyz.aprildown.timer.app.backup.AppPreferenceProviderImpl
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.base.ui.StepUpdater
import xyz.aprildown.timer.app.base.utils.AppPreferenceProvider
import xyz.aprildown.timer.app.timer.edit.UpdateStepDialog
import xyz.aprildown.timer.app.timer.run.MachineService
import xyz.aprildown.timer.presentation.StreamMachineIntentProvider
import xyz.aprildown.timer.presentation.stream.TimerIndex

@Module
@InstallIn(SingletonComponent::class)
abstract class OtherModule {

    @Binds
    abstract fun bindAppPreferenceProvider(impl: AppPreferenceProviderImpl): AppPreferenceProvider

    @Binds
    abstract fun bindAppNavigator(impl: AppNavigatorImpl): AppNavigator

    companion object {

        @Reusable
        @Provides
        fun provideStreamMachineIntentProvider(@ApplicationContext context: Context): StreamMachineIntentProvider {
            return object : StreamMachineIntentProvider {

                override fun bindIntent(): Intent {
                    return MachineService.bindIntent(context)
                }

                override fun startIntent(id: Int, index: TimerIndex?): Intent {
                    return MachineService.startTimingIntent(context, id, index)
                }

                override fun pauseIntent(id: Int): Intent {
                    return MachineService.pauseTimingIntent(context, id)
                }

                override fun decreIntent(id: Int): Intent {
                    return MachineService.decreTimingIntent(context, id)
                }

                override fun increIntent(id: Int): Intent {
                    return MachineService.increTimingIntent(context, id)
                }

                override fun moveIntent(id: Int, index: TimerIndex): Intent {
                    return MachineService.moveTimingIntent(context, id, index)
                }

                override fun resetIntent(id: Int): Intent {
                    return MachineService.resetTimingIntent(context, id)
                }

                override fun adjustTimeIntent(id: Int, amount: Long): Intent {
                    return MachineService.adjustAmountIntent(context, id, amount)
                }
            }
        }

        @Provides
        fun provideStepUpdater(): StepUpdater = UpdateStepDialog
    }
}
