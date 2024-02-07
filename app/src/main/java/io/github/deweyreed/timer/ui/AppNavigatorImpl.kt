package io.github.deweyreed.timer.ui

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.aprildown.timer.app.base.ui.AppNavigator
import xyz.aprildown.timer.app.intro.IntroActivity
import xyz.aprildown.timer.app.timer.edit.EditActivity
import xyz.aprildown.timer.app.timer.list.TimerPicker
import xyz.aprildown.timer.app.timer.one.OneActivity
import xyz.aprildown.timer.app.timer.run.PhantomActivity
import xyz.aprildown.timer.component.key.ImagePreviewActivity
import xyz.aprildown.timer.domain.utils.Constants
import javax.inject.Inject

@Reusable
class AppNavigatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AppNavigator {
    override fun getMainIntent(destId: Int): Intent {
        return MainActivity.intent(context = context, destinationId = destId)
    }

    override fun getEditIntent(timerId: Int, folderId: Long): Intent {
        return Intent(context, EditActivity::class.java)
            .putExtra(Constants.EXTRA_TIMER_ID, timerId)
            .putExtra(EditActivity.EXTRA_FOLDER_ID, folderId)
    }

    override fun getOneIntent(timerId: Int, inNewTask: Boolean): Intent {
        return Intent(context, OneActivity::class.java)
            .putExtra(Constants.EXTRA_TIMER_ID, timerId)
            .apply {
                if (inNewTask) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
    }

    override fun getStartTimerShortcutIntent(timerId: Int, openOnClick: Boolean): Intent {
        return Intent(context, PhantomActivity::class.java)
            .setAction(Constants.ACTION_START)
            .putExtra(Constants.EXTRA_TIMER_ID, timerId)
            .putExtra(PhantomActivity.EXTRA_ONE_SETTING, openOnClick)
    }

    override fun getShortcutCreatedIntent(): Intent {
        return Intent(context, PhantomActivity::class.java)
            .setAction(PhantomActivity.ACTION_SHORTCUT_CREATED)
    }

    override fun getIntroIntent(isOnBoarding: Boolean): Intent {
        return IntroActivity.getIntent(context, isOnBoarding)
    }

    override fun getImagePreviewIntent(path: String): Intent {
        return ImagePreviewActivity.getIntent(context, path)
    }

    override fun pickTimer(
        fm: FragmentManager,
        multiple: Boolean,
        select: List<Int>,
        f: (AppNavigator.PickTimerResult) -> Unit
    ) {
        TimerPicker.createTimerPicker(multiple, select, f).show(fm, null)
    }
}
