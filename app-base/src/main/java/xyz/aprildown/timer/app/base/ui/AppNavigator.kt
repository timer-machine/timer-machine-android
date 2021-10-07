package xyz.aprildown.timer.app.base.ui

import android.content.Intent
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import xyz.aprildown.timer.domain.entities.FolderEntity
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.timer.domain.entities.TimerInfo

interface AppNavigator {
    fun getMainIntent(@IdRes destId: Int = 0): Intent
    fun getEditIntent(
        timerId: Int = TimerEntity.NEW_ID,
        folderId: Long = FolderEntity.FOLDER_DEFAULT
    ): Intent

    fun getOneIntent(timerId: Int, inNewTask: Boolean = false): Intent
    fun getStartTimerShortcutIntent(timerId: Int, openOnClick: Boolean): Intent
    fun getShortcutCreatedIntent(): Intent
    fun getIntroIntent(isOnBoarding: Boolean = false): Intent

    /**
     * @param [timerInfo] is always not empty.
     */
    data class PickTimerResult(
        val timerInfo: List<TimerInfo>,
        val folder: FolderEntity? = null
    )

    fun pickTimer(
        fm: FragmentManager,
        multiple: Boolean = false,
        select: List<Int> = emptyList(),
        f: (PickTimerResult) -> Unit
    )
}
