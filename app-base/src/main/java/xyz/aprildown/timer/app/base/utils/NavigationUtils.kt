package xyz.aprildown.timer.app.base.utils

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import xyz.aprildown.timer.app.base.R

object NavigationUtils {

    fun FragmentManager.getCurrentFragment(@IdRes idRes: Int): Fragment? =
        findFragmentById(idRes)?.childFragmentManager?.primaryNavigationFragment

    fun NavController.subLevelNavigate(@IdRes destId: Int, args: Bundle? = null) {
        navigate(destId, args, createSubLevelNavOption())
    }

    private fun createSubLevelNavOption(): NavOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    fun createMainFragmentNavOptions(@IdRes destId: Int): NavOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .setPopUpTo(destId, false)
        .setEnterAnim(R.anim.open_enter)
        .setExitAnim(R.anim.open_exit)
        .setPopEnterAnim(R.anim.close_enter)
        .setPopExitAnim(R.anim.close_exit)
        .build()
}
