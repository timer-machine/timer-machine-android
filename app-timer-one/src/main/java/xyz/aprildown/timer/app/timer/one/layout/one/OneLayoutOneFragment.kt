package xyz.aprildown.timer.app.timer.one.layout.one

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.github.zawadz88.materialpopupmenu.popupMenu
import xyz.aprildown.timer.app.base.data.PreferenceData
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneFourActions
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneTimeSize
import xyz.aprildown.timer.app.base.data.PreferenceData.oneOneUsingTimingBar
import xyz.aprildown.timer.app.base.data.PreferenceData.timePanels
import xyz.aprildown.timer.app.base.data.ShowcaseData
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.one.FiveActionsView
import xyz.aprildown.timer.app.timer.one.OneFragment
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.timer.app.timer.one.layout.OneLayoutFragment
import xyz.aprildown.timer.app.timer.one.layout.TweakTimeLayout
import xyz.aprildown.timer.app.timer.one.layout.showTimePanelPickerDialog
import xyz.aprildown.timer.app.timer.one.step.StepListView
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.timer.component.key.TimePanelLayout
import xyz.aprildown.timer.component.settings.TweakTimeDialog
import xyz.aprildown.timer.domain.entities.TimerEntity
import xyz.aprildown.tools.anko.dp
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.show

internal class OneLayoutOneFragment :
    Fragment(R.layout.fragment_one),
    OneLayoutFragment.ChildFragment,
    FiveActionsView.Listener,
    TweakTimeLayout.Callback {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context

        view.findViewById<TextView>(R.id.textOneTime).run {
            text = 8158000L.produceTime()
        }
        view.findViewById<TextView>(R.id.textOneLoop).text = "1/3"
        view.findViewById<StepListView>(R.id.listOneSteps).run {
            setHasFixedSize(true)
            setTimer(TimerEntity(1, "", 1, ShowcaseData.getSampleSteps()))
        }
        view.findViewById<FiveActionsView>(R.id.fiveActionsOne).run {
            setActionClickListener(this@OneLayoutOneFragment)
        }
        view.findViewById<TweakTimeLayout>(R.id.layoutOneTweakTime)
            .setCallback(requireActivity(), this)

        toggleTimingBar(context.oneOneUsingTimingBar)
        setTimeTextSize(context.oneOneTimeSize)
        toggleTimePanels()
        setFourActions()
    }

    override fun onActionClick(index: Int, view: View) {
        val context = view.context
        popupMenu {
            section {
                OneFragment.getFourActionsFromKeys(
                    listOf(
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_STOP,
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_PREV,
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_NEXT,
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_MORE,
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_LOCK,
                        PreferenceData.ONE_LAYOUT_ONE_ACTION_EDIT
                    )
                ).forEach { action ->
                    item {
                        label = context.getString(action.nameRes)
                        icon = action.defaultDrawableRes
                        callback = {
                            context.oneOneFourActions =
                                context.oneOneFourActions.toMutableList().apply {
                                    set(index, action.tag)
                                }
                            setFourActions()
                        }
                    }
                }
            }
        }.show(context, view)
    }

    override fun provideEditDialogView(): View {
        val context = requireContext()
        val view = View.inflate(context, R.layout.layout_one_settings_one, null)

        view.findViewById<ListItemWithLayout>(R.id.itemOneLayoutOneBar).run {
            getLayoutView<CompoundButton>().run {
                isChecked = context.oneOneUsingTimingBar
                setOnCheckedChangeListener { _, isChecked ->
                    toggleTimingBar(isChecked)
                    context.oneOneUsingTimingBar = isChecked
                }
            }
        }

        view.findViewById<SeekBar>(R.id.seekOneLayoutOneTimeSize).run {
            progress = context.oneOneTimeSize
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    context.oneOneTimeSize = seekBar.progress
                }

                override fun onProgressChanged(
                    seekBar: SeekBar,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    setTimeTextSize(progress)
                }
            })
        }

        view.findViewById<View>(R.id.itemOneLayoutOneTimePanels).run {
            setOnClickListener {
                context.showTimePanelPickerDialog { toggleTimePanels() }
            }
        }

        return view
    }

    private fun toggleTimingBar(show: Boolean) {
        val view = requireView()
        val stub: ViewStub? = view.findViewById(R.id.stubTimingBar)
        var progress: ProgressBar? = view.findViewById(R.id.progressTimingBar)
        if (show) {
            if (progress == null) {
                progress = stub?.inflate() as ProgressBar
            }
            progress.run {
                show()
                this.progress = 50
            }
        } else {
            progress?.gone()
        }
    }

    private fun setTimeTextSize(size: Int) {
        view?.findViewById<TextView>(R.id.textOneTime)?.textSize = dp(size)
    }

    private fun toggleTimePanels() {
        val view = requireView()
        val currentTimePanels = requireContext().timePanels
        val stub: ViewStub? = view.findViewById(R.id.stubTimePanel)
        var timePanelLayout: TimePanelLayout? = view.findViewById(R.id.layoutTimePanel)
        if (currentTimePanels.isNotEmpty()) {
            if (timePanelLayout == null) {
                timePanelLayout = stub?.inflate() as TimePanelLayout
            }
            timePanelLayout.run {
                show()
                setPanels(currentTimePanels)
            }
        } else {
            timePanelLayout?.gone()
        }
    }

    private fun toggleTweakTimeLayout() {
        val view = requireView().findViewById<TweakTimeLayout>(R.id.layoutOneTweakTime)
        val lp = view.layoutParams as ConstraintLayout.LayoutParams
        val parent = view.parent as ViewGroup
        parent.removeView(view)
        parent.addView(
            TweakTimeLayout(requireContext()).apply {
                id = R.id.layoutOneTweakTime
                setCallback(requireActivity(), this@OneLayoutOneFragment)
            },
            lp
        )
    }

    override fun onTweakTime(amount: Long) {
        TweakTimeDialog().show(requireContext()) {
            toggleTweakTimeLayout()
        }
    }

    private fun setFourActions() {
        requireView().findViewById<FiveActionsView>(R.id.fiveActionsOne).run {
            withActions(OneFragment.getFourActionsFromKeys(context.oneOneFourActions))
        }
    }
}
