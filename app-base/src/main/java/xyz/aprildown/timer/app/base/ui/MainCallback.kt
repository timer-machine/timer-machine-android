package xyz.aprildown.timer.app.base.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface MainCallback {
    interface ActivityCallback {
        val actionFab: FloatingActionButton
        val snackbarView: View
        fun enterTimerScreen(itemView: View, id: Int)
        fun enterEditScreen(timerId: Int, folderId: Long)
        fun restartWithDestination(
            @IdRes destinationId: Int,
            destinationArguments: Bundle = Bundle.EMPTY
        )

        fun recreateThemeItem()
    }

    interface FragmentCallback {
        fun onFabClick(view: View)
    }
}
