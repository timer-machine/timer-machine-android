package io.github.deweyreed.timer

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication
import xyz.aprildown.theme.Theme
import xyz.aprildown.timer.app.base.R as RBase

/**
 * We use a separate App for tests to prevent initializing dependency injection.
 */
open class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Theme.init(context = this, themeRes = RBase.style.AppTheme)
    }
}

@CustomTestApplication(TestApp::class)
interface HiltTestApplication
