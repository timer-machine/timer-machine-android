package xyz.aprildown.timer.app.base.ui

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier

interface FlavorUiInjector {
    fun showInAppPurchases(activity: FragmentActivity)
    val cloudBackupNavGraphId: Int
    fun toCloudBackupFragment(currentFragment: Fragment)
    fun toBakedCountDialog(fragment: Fragment)
    fun useMoreTheme(fragment: Fragment, onApply: () -> Unit)
    fun onMainActivityCreated(activity: ComponentActivity)
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class FlavorUiInjectorQualifier

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FlavorUiInjectorModule {
    @BindsOptionalOf
    @FlavorUiInjectorQualifier
    abstract fun bindOptionalFlavorUiInjector(): FlavorUiInjector
}
