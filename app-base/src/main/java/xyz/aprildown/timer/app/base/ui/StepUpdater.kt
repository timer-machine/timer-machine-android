package xyz.aprildown.timer.app.base.ui

import androidx.fragment.app.DialogFragment
import xyz.aprildown.timer.domain.entities.StepEntity

interface StepUpdater {
    fun updateStep(step: StepEntity.Step, onUpdate: (StepEntity.Step) -> Unit): DialogFragment
}
