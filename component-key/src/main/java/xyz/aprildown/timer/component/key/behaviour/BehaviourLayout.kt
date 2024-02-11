package xyz.aprildown.timer.component.key.behaviour

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import coil.load
import com.github.deweyreed.tools.helper.setTextIfChanged
import com.google.android.material.chip.Chip
import xyz.aprildown.timer.component.key.R
import xyz.aprildown.timer.component.key.databinding.LayoutBehaviourBinding
import xyz.aprildown.timer.component.key.databinding.LayoutEditableBehaviourImageBinding
import xyz.aprildown.timer.domain.entities.BehaviourEntity
import xyz.aprildown.timer.domain.entities.BehaviourType
import xyz.aprildown.timer.domain.entities.ImageAction
import xyz.aprildown.timer.domain.entities.toImageAction

class BehaviourLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val binding = LayoutBehaviourBinding.inflate(LayoutInflater.from(context), this)

    private val currentBehaviours = mutableListOf<BehaviourChipView>()

    var onImageCheck: ((ImageAction) -> Unit)? = null

    init {
        orientation = VERTICAL
    }

    fun setBehaviours(list: List<BehaviourEntity>) {
        val currentSize = currentBehaviours.size

        val removeCount = currentBehaviours.size - list.size
        if (removeCount > 0) {
            repeat(removeCount) {
                val index = currentSize - it - 1
                val chipView = currentBehaviours[index]
                binding.chipGroup.removeView(chipView.chip)
                currentBehaviours.remove(chipView)
            }
        }

        require(currentBehaviours.size <= list.size)

        list.forEachIndexed { index, behaviourEntity ->
            if (index < currentSize) {
                currentBehaviours[index].changeBehaviour(behaviourEntity)
            } else {
                currentBehaviours.add(BehaviourChipView(createNewChip(), behaviourEntity))
            }
        }

        require(currentBehaviours.size == list.size)

        val imageBehaviour = list.find { it.type == BehaviourType.IMAGE }
        if (imageBehaviour != null) {
            binding.layoutImage.isVisible = true
            val imageView = if (binding.layoutImage.isEmpty()) {
                LayoutEditableBehaviourImageBinding.inflate(
                    LayoutInflater.from(context), binding.layoutImage, true
                ).root
            } else {
                LayoutEditableBehaviourImageBinding.bind(binding.layoutImage.getChildAt(0)).root
            }
            val action = imageBehaviour.toImageAction()
            imageView.setOnClickListener { onImageCheck?.invoke(action) }
            imageView.load(action.path) {
                crossfade(true)
            }
        } else {
            binding.layoutImage.isVisible = false
            binding.layoutImage.removeAllViews()
        }
    }

    fun setEnabledColor(@ColorInt color: Int) {
        currentBehaviours.forEach {
            it.updateBackground(color)
        }
    }

    private fun createNewChip(): Chip {
        val chip = View.inflate(context, R.layout.view_behavior_chip, null) as Chip
        binding.chipGroup.addView(chip)
        return chip
    }

    private class BehaviourChipView(
        val chip: Chip,
        private var behaviour: BehaviourEntity
    ) {

        private val context = chip.context

        init {
            chip.isCloseIconVisible = false
            updateChipView()
        }

        fun changeBehaviour(new: BehaviourEntity) {
            behaviour = new
            updateChipView()
        }

        fun updateChipView() {
            val type = behaviour.type
            chip.setChipIconResource(type.iconRes)
            chip.setTextIfChanged(behaviour.getChipText(context))
            TooltipCompat.setTooltipText(chip, context.getString(type.despRes))
        }

        fun updateBackground(@ColorInt color: Int) {
            chip.chipBackgroundColor = ColorStateList.valueOf(color)
        }
    }
}
