package xyz.aprildown.timer.app.backup

import ernestoyaquello.com.verticalstepperform.Step

internal abstract class AbstractStep<T>(
    title: String,
    subTitle: String = "",
    nextButtonText: String = ""
) : Step<T>(title, subTitle, nextButtonText) {
    override fun restoreStepData(data: T) = Unit

    override fun onStepMarkedAsCompleted(animated: Boolean) = Unit

    override fun onStepOpened(animated: Boolean) = Unit

    override fun onStepMarkedAsUncompleted(animated: Boolean) = Unit

    override fun onStepClosed(animated: Boolean) = Unit

    override fun getStepDataAsHumanReadableString(): String = ""
}
