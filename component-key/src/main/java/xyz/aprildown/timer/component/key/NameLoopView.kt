package xyz.aprildown.timer.component.key

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import com.google.android.material.textfield.TextInputLayout
import xyz.aprildown.tools.helper.showActionAndMultiLine
import xyz.aprildown.tools.helper.themeColor
import xyz.aprildown.tools.helper.toColorStateList
import com.google.android.material.R as RMaterial

class NameLoopView(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val nameInputLayout: TextInputLayout
    val nameView: EditText
    private val loopInputLayout: TextInputLayout
    val loopView: EditText

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        View.inflate(context, R.layout.layout_name_loop, this)

        nameInputLayout = findViewById(R.id.inputNameLoopName)
        nameView = findViewById(R.id.editNameLoopName)
        nameView.showActionAndMultiLine(EditorInfo.IME_ACTION_NEXT)
        loopInputLayout = findViewById(R.id.inputNameLoopLoop)
        loopView = findViewById(R.id.editNameLoopLoop)

        context.withStyledAttributes(attrs, R.styleable.NameLoopView) {
            val color = getColor(
                R.styleable.NameLoopView_nlv_view_color,
                context.themeColor(RMaterial.attr.colorOnPrimary)
            )
            withColor(color)
        }
    }

    fun getName(): String = nameView.text.toString()
    fun setName(name: String) {
        nameView.setText(name)
    }

    fun getLoop(): Int = loopView.text.toString().toIntOrNull() ?: 0
    fun setLoop(loop: Int) {
        loopView.setText(loop.toString())
    }

    fun withColor(@ColorInt textColor: Int) {
        val csl = textColor.toColorStateList()
        nameInputLayout.defaultHintTextColor = csl
        nameInputLayout.setHelperTextColor(csl)
        nameInputLayout.boxStrokeColor = textColor
        nameView.setTextColor(textColor)
        loopInputLayout.defaultHintTextColor = csl
        loopInputLayout.setHelperTextColor(csl)
        loopInputLayout.boxStrokeColor = textColor
        loopView.setTextColor(textColor)
    }
}
