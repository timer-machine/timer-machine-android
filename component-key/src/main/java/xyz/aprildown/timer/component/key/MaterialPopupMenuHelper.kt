package xyz.aprildown.timer.component.key

import android.view.View
import android.widget.CompoundButton
import com.github.zawadz88.materialpopupmenu.MaterialPopupMenuBuilder
import xyz.aprildown.timer.component.key.databinding.MpmPopupMenuSwitchBinding

fun MaterialPopupMenuBuilder.SectionHolder.switchItem(init: MaterialPopupMenuCompoundButtonHolder.() -> Unit) {
    val holder = MaterialPopupMenuCompoundButtonHolder()
    holder.init()
    customItem {
        layoutResId = R.layout.mpm_popup_menu_switch
        viewBoundCallback = { view ->
            val binding = MpmPopupMenuSwitchBinding.bind(view)
            binding.mpmSwitchLabel.text = holder.label
            val switch = binding.mpmSwitchSwitch as CompoundButton
            holder.onBind.invoke(switch)
            switch.setOnCheckedChangeListener { buttonView, isChecked ->
                holder.onCheckedChange.invoke(buttonView, isChecked)
            }
            binding.layoutPopupMenuSwitch.setOnClickListener {
                switch.toggle()
            }
        }
        dismissOnSelect = false
    }
}

class MaterialPopupMenuCompoundButtonHolder {
    var label: String = ""
    var onBind: (CompoundButton) -> Unit = {}
    var onCheckedChange: (View, Boolean) -> Unit = { _, _ -> }
}
