package xyz.aprildown.timer.app.timer.edit

import android.text.Editable
import android.text.TextWatcher
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.adapters.ListenerUtil
import xyz.aprildown.timer.component.key.NameLoopView

@BindingAdapter("name")
internal fun NameLoopView.setNameBinding(name: String?) {
    if (getName() != name) {
        setName(name ?: "")
    }
}

@InverseBindingAdapter(attribute = "name")
internal fun NameLoopView.getNameBinding(): String = getName()

@BindingAdapter("nameAttrChanged")
internal fun NameLoopView.setNameChangeListener(timeAttrChanged: InverseBindingListener) {
    val newValue = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            timeAttrChanged.onChange()
        }
    }
    val oldValue = ListenerUtil.trackListener(this, newValue, R.id.listener_name_loop_name)
    if (oldValue != null) {
        nameView.removeTextChangedListener(oldValue)
    }
    nameView.addTextChangedListener(newValue)
}

// endregion name binding adapters

// region loop binding adapters

@BindingAdapter("loop")
internal fun NameLoopView.setLoopBinding(loop: Int?) {
    if (getLoop() != loop) {
        setLoop(loop ?: 0)
    }
}

@InverseBindingAdapter(attribute = "loop")
internal fun NameLoopView.getLoopBinding(): Int = getLoop()

@BindingAdapter("loopAttrChanged")
internal fun NameLoopView.setLoopChangeListener(timeAttrChanged: InverseBindingListener) {
    val newValue = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            timeAttrChanged.onChange()
        }
    }
    val oldValue = ListenerUtil.trackListener(this, newValue, R.id.listener_name_loop_loop)
    if (oldValue != null) {
        loopView.removeTextChangedListener(oldValue)
    }
    loopView.addTextChangedListener(newValue)
}
