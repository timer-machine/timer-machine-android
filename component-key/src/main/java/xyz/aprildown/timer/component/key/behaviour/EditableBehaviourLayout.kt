package xyz.aprildown.timer.component.key.behaviour

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompat
import androidx.appcompat.widget.TooltipCompatFix
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import coil.load
import com.github.deweyreed.tools.helper.drawable
import com.github.deweyreed.tools.helper.setTextIfChanged
import com.github.deweyreed.tools.helper.toColorStateList
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import xyz.aprildown.timer.component.key.R
import xyz.aprildown.timer.component.key.databinding.LayoutEditableBehaviourBinding
import xyz.aprildown.timer.component.key.databinding.LayoutEditableBehaviourImageBinding
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.toImageAction

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

        fun onImageAdding(): Unit = Unit
        fun onImageContentClick(action: ImageAction): Unit = Unit
    }

    private val binding =
        LayoutEditableBehaviourBinding.inflate(LayoutInflater.from(context), this)

    @ColorInt
    private var colorEnabled: Int = Color.RED

    private val data = LinkedHashMap<BehaviourType, Pair<Chip, BehaviourEntity>>()
    private var listener: Listener? = null
    private var onBehaviourAddedOrRemovedCallback: (() -> Unit)? = null

    val addButton: ImageButton get() = binding.btnBehaviourAdd

    private val enabledBehaviourTypes = BehaviourType.entries

    init {
        flexWrap = FlexWrap.WRAP
        setShowDivider(SHOW_DIVIDER_MIDDLE)
        setDividerDrawable(context.drawable(R.drawable.divider_normal_flexbox))

        binding.btnBehaviourAdd.setOnClickListener { view ->
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
                                    if (type == BehaviourType.IMAGE) {
                                        listener?.onImageAdding()
                                    } else {
                                        addStubBehaviour(BehaviourEntity(type), true)
                                    }
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

        val imageAction = bs.find { it.type == BehaviourType.IMAGE }?.toImageAction()
        if (imageAction != null) {
            val imageView = if (binding.layoutImage.isEmpty()) {
                LayoutEditableBehaviourImageBinding.inflate(
                    LayoutInflater.from(context), binding.layoutImage, true
                ).root
            } else {
                LayoutEditableBehaviourImageBinding.bind(binding.layoutImage.getChildAt(0)).root
            }
            imageView.setOnClickListener { listener?.onImageContentClick(imageAction) }
            imageView.load(imageAction.path) {
                crossfade(true)
            }
        } else {
            binding.layoutImage.removeAllViews()
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
        addView(newView, indexOfChild(binding.layoutBottomBar))
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

        if (behaviourType == BehaviourType.IMAGE) {
            binding.layoutImage.removeAllViews()
        }

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
        binding.btnBehaviourAdd.isVisible = dataSize < enabledBehaviourTypes.size
        binding.textBehaviourEmpty.isVisible = dataSize == 0
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
