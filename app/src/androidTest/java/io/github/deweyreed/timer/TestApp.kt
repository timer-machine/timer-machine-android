package io.github.deweyreed.timer

import android.app.Application
import dagger.hilt.android.testing.CustomTestApplication
import xyz.aprildown.theme.Theme

/**
 * We use a separate App for tests to prevent initializing dependency injection.
 */
open class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Theme.init(context = this, themeRes = R.style.AppTheme)
    }
}

@CustomTestApplication(TestApp::class)
interface HiltTestApplication
