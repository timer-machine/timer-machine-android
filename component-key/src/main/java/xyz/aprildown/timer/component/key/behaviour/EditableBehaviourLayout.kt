package xyz.aprildown.timer.component.key.behaviour

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompat
import androidx.appcompat.widget.TooltipCompatFix
import com.github.deweyreed.tools.helper.drawable
import com.github.deweyreed.tools.helper.gone
import com.github.deweyreed.tools.helper.setTextIfChanged
import com.github.deweyreed.tools.helper.show
import com.github.deweyreed.tools.helper.toColorStateList
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import xyz.aprildown.timer.component.key.R
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType

class EditableBehaviourLayout(
    context: Context,
    attrs: AttributeSet? = null
) : FlexboxLayout(context, attrs) {

    interface Listener {
        fun onBehaviourListShow(): Unit = Unit
        fun showBehaviourSettingsView(
            view: View,
            layout: EditableBehaviourLayout,
            current: BehaviourEntity
        )
    }

    @ColorInt
    private var colorEnabled: Int = Color.RED

    private val data = LinkedHashMap<BehaviourType, Pair<Chip, BehaviourEntity>>()
    private var listener: Listener? = null
    private var onBehaviourAddedOrRemovedCallback: (() -> Unit)? = null

    val addButton: ImageButton
    private val emptyText: TextView

    private val enabledBehaviourTypes = BehaviourType.entries

    init {
        flexWrap = FlexWrap.WRAP
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        setDividerDrawable(context.drawable(R.drawable.divider_normal_flexbox))

        View.inflate(context, R.layout.layout_editable_behaviour, this)

        emptyText = findViewById(R.id.textBehaviourEmpty)
        addButton = findViewById(R.id.btnBehaviourAdd)
        addButton.setOnClickListener { view ->
            val currentTypes = data.keys
            val showTypes = enabledBehaviourTypes.filter { type -> type !in currentTypes }
            if (showTypes.isNotEmpty()) {
                popupMenu {
                    dropdownGravity = Gravity.TOP or Gravity.END
                    section {
                        showTypes.forEach { type ->
                            item {
                                label = context.getString(type.nameRes)
                                icon = type.iconRes
                                viewBoundCallback = {
                                    TooltipCompatFix.setTooltipText(
                                        it,
                                        context.getString(type.despRes)
                                    )
                                }
                                callback = {
                                    addStubBehaviour(BehaviourEntity(type), true)
                                }
                            }
                        }
                    }
                }.show(context, view)
                listener?.onBehaviourListShow()
            }
        }
    }

    fun setListener(l: Listener) {
        listener = l
    }

    fun setBehaviourAddedOrRemovedCallback(c: (() -> Unit)?) {
        onBehaviourAddedOrRemovedCallback = c
    }

    /**
     * Must called before [setBehaviours]
     */
    fun setEnabledColor(@ColorInt color: Int) {
        colorEnabled = color
    }

    fun getBehaviours(): List<BehaviourEntity> = data.map { it.value.second }

    fun setBehaviours(bs: List<BehaviourEntity>) {
        data.values.forEach {
            removeView(it.first)
        }
        data.clear()

        bs.forEach {
            addStubBehaviour(it, false)
            setViewWithEntity(it)
        }
        updateHelperViews()
    }

    /**
     * @param fromUser If this method is called from clicks of user
     */
    private fun addStubBehaviour(behaviour: BehaviourEntity, fromUser: Boolean) {
        val type = behaviour.type
        removeBehaviour(type)
        val newView = generateBehaviourView(type)
        addView(newView, childCount - 1)
        data[type] = newView to behaviour

        if (fromUser) {
            // If not fromUser, we update after add all behaviours.
            updateHelperViews()
            onBehaviourAddedOrRemovedCallback?.invoke()
        }
    }

    fun removeBehaviour(behaviourType: BehaviourType) {
        val (view, _) = data[behaviourType] ?: return
        removeView(view)
        data.remove(behaviourType)
        updateHelperViews()

        onBehaviourAddedOrRemovedCallback?.invoke()
    }

    private fun setViewWithEntity(entity: BehaviourEntity) {
        val type = entity.type
        val view = data[entity.type]?.first ?: return
        view.setTextIfChanged(entity.getChipText(context))
        data[type] = view to entity
    }

    private fun generateBehaviourView(type: BehaviourType): Chip {
        val view = createNewBehaviourChip(type, colorEnabled)
        val onClick = OnClickListener {
            listener?.showBehaviourSettingsView(
                it,
                this,
                data[type]?.second ?: error("BehaviourView has a null behaviour $data")
            )
        }
        view.setOnCloseIconClickListener(onClick)
        view.setOnClickListener(onClick)
        return view
    }

    private fun updateHelperViews() {
        val dataSize = data.size
        if (dataSize >= enabledBehaviourTypes.size) addButton.gone() else addButton.show()
        if (dataSize == 0) emptyText.show() else emptyText.gone()
    }

    private fun createNewBehaviourChip(
        behaviourType: BehaviourType,
        @ColorInt chipColor: Int
    ): Chip = (View.inflate(context, R.layout.view_behavior_chip, null) as Chip).apply {
        setChipIconResource(behaviourType.iconRes)
        setText(behaviourType.nameRes)
        chipBackgroundColor = chipColor.toColorStateList()
        TooltipCompat.setTooltipText(this, context.getString(behaviourType.despRes))
    }
}
