package xyz.aprildown.timer.app.timer.one

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import xyz.aprildown.tools.helper.startDrawableAnimation
import xyz.aprildown.timer.app.base.R as RBase

typealias ButtonImageView = Pair<View, ImageView>

class FiveActionsView constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    interface Listener {
        fun onActionClick(index: Int, view: View)
    }

    class Action(
        val tag: String,
        @StringRes val nameRes: Int,
        @DrawableRes val defaultDrawableRes: Int
    )

    private val bg: View
    private val mainFab: FloatingActionButton

    private val view1: ButtonImageView
    private val view2: ButtonImageView
    private val view3: ButtonImageView
    private val view4: ButtonImageView

    private var currentState = STATE_PLAY

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_five_actions, this).apply {
            bg = findViewById(R.id.viewFiveBackground)
            mainFab = findViewById(R.id.fabFiveMain)
            view1 = ButtonImageView(
                findViewById(R.id.frameFiveAction1),
                findViewById(R.id.imageFiveAction1)
            )
            view2 = ButtonImageView(
                findViewById(R.id.frameFiveAction2),
                findViewById(R.id.imageFiveAction2)
            )
            view3 = ButtonImageView(
                findViewById(R.id.frameFiveAction3),
                findViewById(R.id.imageFiveAction3)
            )
            view4 = ButtonImageView(
                findViewById(R.id.frameFiveAction4),
                findViewById(R.id.imageFiveAction4)
            )
        }
    }

    fun setMainFabClickListener(listener: OnClickListener) {
        mainFab.setOnClickListener(listener)
    }

    fun setActionClickListener(listener: Listener) {
        getViews().forEachIndexed { index, pair ->
            pair.first.setOnClickListener {
                listener.onActionClick(index, it)
            }
        }
    }

    fun changeState(newState: Int) {
        if (newState != currentState) {
            val oldState = currentState
            currentState = newState
            when {
                oldState == STATE_PLAY && newState == STATE_PAUSE -> {
                    changeMainFab(RBase.drawable.ic_anim_play_to_pause, RBase.string.pause)
                }
                oldState == STATE_PAUSE && newState == STATE_PLAY -> {
                    changeMainFab(RBase.drawable.ic_anim_pause_to_play, RBase.string.start)
                }
            }
            mainFab.startDrawableAnimation()
        }
    }

    private fun changeMainFab(@DrawableRes drawableRes: Int, @StringRes contentDespRes: Int) {
        mainFab.setImageResource(drawableRes)
        mainFab.contentDescription = context.getString(contentDespRes)
    }

    fun withActions(actions: List<Action>) {
        require(actions.size == 4)
        val context = context
        fun inflateViews(view: ButtonImageView, action: Action) {
            view.first.tag = action.tag
            view.second.setImageResource(action.defaultDrawableRes)
            view.second.contentDescription = context.getString(action.nameRes)
        }
        inflateViews(view1, actions[0])
        inflateViews(view2, actions[1])
        inflateViews(view3, actions[2])
        inflateViews(view4, actions[3])
    }

    fun changeAction(tag: String, @StringRes nameRes: Int, @DrawableRes drawableRes: Int) {
        getViews().find { it.first.tag == tag }?.second?.run {
            contentDescription = context.getString(nameRes)
            setImageResource(drawableRes)
        }
    }

    private fun getViews() = listOf(view1, view2, view3, view4)

    companion object {
        const val STATE_PLAY = 0
        const val STATE_PAUSE = 1
    }
}
