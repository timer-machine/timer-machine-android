package xyz.aprildown.timer.app.timer.edit.voice

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.MultiAutoCompleteTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import xyz.aprildown.timer.app.timer.edit.R
import xyz.aprildown.timer.app.timer.edit.databinding.LayoutVoiceVariableContentBinding

internal class VoiceVariableContentView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    val binding =
        LayoutVoiceVariableContentBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.root.doOnPreDraw {
            binding.layoutUsage.root.updateLayoutParams {
                width = binding.inputLayout.width
            }
        }

        binding.edit.run {
            setAdapter(
                ArrayAdapter(
                    context,
                    R.layout.list_item_voice_variable_auto_completion,
                    binding.table.allVariables
                )
            )
            setTokenizer(VariableTokenizer())
        }
        binding.table.onVariableClicked = { variable ->
            binding.edit.run {
                if (selectionStart == selectionEnd) {
                    text?.insert(selectionStart, variable)
                } else {
                    text?.replace(selectionStart, selectionEnd, variable)
                }
            }
        }
    }
}

private class VariableTokenizer : MultiAutoCompleteTextView.Tokenizer {

    private val tokens = setOf(' ', '，', '。')

    override fun findTokenStart(text: CharSequence, cursor: Int): Int {
        var i = cursor
        while (i > 0 && text[i - 1] !in tokens) {
            i--
        }
        while (i < cursor && text[i] == ' ') {
            i++
        }
        return i
    }

    override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
        var i = cursor
        val len = text.length
        while (i < len) {
            if (text[i] in tokens) {
                return i
            } else {
                i++
            }
        }
        return len
    }

    override fun terminateToken(text: CharSequence): CharSequence {
        return text
    }
}
