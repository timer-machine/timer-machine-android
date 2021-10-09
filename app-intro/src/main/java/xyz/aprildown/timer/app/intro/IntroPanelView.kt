package xyz.aprildown.timer.app.intro

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import xyz.aprildown.timer.app.intro.databinding.ViewIntroPanelBinding
import xyz.aprildown.tools.helper.startDrawableAnimation
import xyz.aprildown.tools.helper.stopDrawableAnimation
import xyz.aprildown.tools.helper.themeColor
import com.google.android.material.R as RMaterial
import xyz.aprildown.timer.app.base.R as RBase

internal class IntroPanelView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    interface Callback {
        fun onNextInstruction()
        fun onPreviousInstruction()
    }

    private val binding: ViewIntroPanelBinding
    var callback: Callback? = null

    init {
        setBackgroundColor(context.themeColor(RMaterial.attr.colorSurface))
        View.inflate(context, R.layout.view_intro_panel, this)
        binding = ViewIntroPanelBinding.bind(this)

        binding.btnIntroPanelNext.setOnClickListener {
            callback?.onNextInstruction()
        }
        binding.btnIntroPanelPrevious.setOnClickListener {
            callback?.onPreviousInstruction()
        }
    }

    fun withInstruction(instruction: Instruction<out ViewBinding>, progressText: CharSequence) {
        binding.run {
            textIntroPanel.setText(instruction.despRes)
            textIntroPanelProgress.text = progressText
            btnIntroPanelNext.isVisible = !instruction.requireAction
        }
    }

    fun withPreviousOrExit(hasPrevious: Boolean, hasNext: Boolean) {
        binding.btnIntroPanelPrevious.contentDescription = context.getString(
            if (hasPrevious) {
                RBase.string.previous
            } else {
                RBase.string.exit
            }
        )
        binding.btnIntroPanelNext.contentDescription = context.getString(
            if (hasNext) {
                RBase.string.next
            } else {
                RBase.string.exit
            }
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.btnIntroPanelNext.startDrawableAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.btnIntroPanelNext.stopDrawableAnimation()
    }
}
