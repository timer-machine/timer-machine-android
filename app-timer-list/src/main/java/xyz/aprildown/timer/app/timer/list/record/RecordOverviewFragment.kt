package xyz.aprildown.timer.app.timer.list.record

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.FragmentRecordOverviewBinding
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.presentation.timer.RecordViewModel
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.gone
import xyz.aprildown.tools.helper.setTextIfChanged
import xyz.aprildown.tools.helper.show
import xyz.aprildown.tools.helper.themeColor

internal class RecordOverviewFragment : Fragment(R.layout.fragment_record_overview) {

    private val viewModel: RecordViewModel? get() = (parentFragment as? RecordFragment)?.viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentRecordOverviewBinding.bind(view)

        binding.chartRecordTotalTime.applyCommonSettings()
        binding.chartRecordTotalCount.applyCommonSettings()

        viewModel?.overview?.let { overview ->
            overview.observe(viewLifecycleOwner, { signal ->
                when (signal) {
                    is GetRecords.Signal.Processing -> {
                        binding.layoutRecordContent.animateHideGraphs()
                        binding.progressRecordLoading.show()
                    }
                    is GetRecords.Signal.Result -> {
                        binding.layoutRecordContent.animateShowGraphs()
                        binding.progressRecordLoading.hide()

                        fun populateChart(
                            chart: PieChart,
                            entries: List<PieEntry>,
                            labelGetter: (Any) -> String
                        ) {
                            if (entries.size <= 1 || entries.all { it.value == 0f }) {
                                chart.gone()
                                return
                            } else {
                                chart.show()
                            }

                            val dataSet = PieDataSet(entries, "")
                            dataSet.applyCommonSettings()
                            val data = PieData(dataSet)
                            data.setValueFormatter(object : ValueFormatter() {
                                override fun getPieLabel(
                                    value: Float,
                                    pieEntry: PieEntry?
                                ): String {
                                    val bundle = pieEntry?.data
                                    return if (bundle != null) labelGetter.invoke(bundle) else ""
                                }
                            })

                            chart.run {
                                this.data = data
                                highlightValue(null)
                                animateY(750, Easing.EaseInOutQuad)
                            }
                        }

                        val result = signal.result
                        val timeData = result.timeData

                        binding.textRecordTotalTime.setTextIfChanged(
                            timeData.values.fold(0L) { acc, recordBundle ->
                                acc + recordBundle.data
                            }.produceTime()
                        )
                        populateChart(
                            chart = binding.chartRecordTotalTime,
                            entries = timeData.map { pair ->
                                val timerId = pair.key
                                val percent = pair.value.percent
                                PieEntry(
                                    percent,
                                    if (timerId == null) {
                                        getString(R.string.record_other_data)
                                    } else {
                                        viewModel?.queryTimerName(timerId).toString()
                                    },
                                    percent
                                )
                            },
                            labelGetter = { "%.2f%%".format(((it as? Float) ?: 0f) * 100) }
                        )

                        val countData = result.countData

                        binding.textRecordTotalCount.setTextIfChanged(
                            countData.values.fold(0) { acc, recordBundle ->
                                acc + recordBundle.data
                            }.toString()
                        )
                        populateChart(
                            chart = binding.chartRecordTotalCount,
                            entries = countData.map { pair ->
                                val timerId = pair.key
                                val value = pair.value
                                PieEntry(
                                    value.percent,
                                    if (timerId == null) {
                                        getString(R.string.record_other_data)
                                    } else {
                                        viewModel?.queryTimerName(timerId).toString()
                                    },
                                    value
                                )
                            },
                            labelGetter = {
                                val entry = it as? GetRecords.OverviewResult.Entry<*>
                                if (entry != null) {
                                    "%.2f%%".format(entry.percent * 100) + "(${(it.data)})"
                                } else {
                                    ""
                                }
                            }
                        )
                    }
                    else -> Unit
                }
            })
        }
    }

    private fun PieChart.applyCommonSettings() {
        description.isEnabled = false
        setUsePercentValues(true)
        legend.isEnabled = false
        setHoleColor(Color.TRANSPARENT)
        setEntryLabelColor(requireContext().themeColor(android.R.attr.textColorPrimary))
        setEntryLabelTextSize(11f)
        setExtraOffsets(8f, 8f, 8f, 8f)
    }

    private fun PieDataSet.applyCommonSettings() {
        colors = getChartColors()
        sliceSpace = 3f
        xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        valueTextSize = 11f
        selectionShift = 5f
        val valueColor = requireContext().themeColor(android.R.attr.textColorPrimary)
        valueLineColor = valueColor
        valueTextColor = valueColor
        valueLinePart1Length = 0.4f
        valueLinePart2Length = 0.4f
        valueLinePart1OffsetPercentage = 100f
    }

    private fun getChartColors(): List<Int> {
        val context = requireContext()
        return listOf(
            context.color(R.color.md_red_500),
            context.color(R.color.md_amber_700),
            context.color(R.color.md_light_green_700),
            context.color(R.color.md_light_blue_600),
            context.color(R.color.md_brown_500),
            context.color(R.color.md_green_500),
            context.color(R.color.md_indigo_500),
            context.color(R.color.md_teal_500),
            context.color(R.color.md_blue_grey_500)
        )
    }
}
