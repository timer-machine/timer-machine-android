package xyz.aprildown.timer.app.timer.list.record

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import xyz.aprildown.timer.app.base.ui.newDynamicTheme
import xyz.aprildown.timer.app.base.utils.produceTime
import xyz.aprildown.timer.app.timer.list.R
import xyz.aprildown.timer.app.timer.list.databinding.FragmentRecordTimelineBinding
import xyz.aprildown.timer.domain.usecases.record.GetRecords
import xyz.aprildown.timer.presentation.timer.RecordViewModel
import xyz.aprildown.tools.anko.dp
import xyz.aprildown.tools.helper.themeColor
import java.util.Date

internal class RecordTimelineFragment : Fragment(R.layout.fragment_record_timeline) {

    private val viewModel: RecordViewModel? get() = (parentFragment as? RecordFragment)?.viewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentRecordTimelineBinding.bind(view)

        binding.chartRecordTime.applyCommonSettings(
            durationFormatter = { it.produceTime() }
        )

        binding.chartRecordCount.applyCommonSettings(
            durationFormatter = { it.toString() }
        )

        viewModel?.timeline?.observe(viewLifecycleOwner, { signal ->
            when (signal) {
                is GetRecords.Signal.Processing -> {
                    binding.layoutRecordContent.animateHideGraphs()
                    binding.progressRecordLoading.show()
                }
                is GetRecords.Signal.Result -> {
                    binding.layoutRecordContent.animateShowGraphs()
                    binding.progressRecordLoading.hide()

                    val result = signal.result
                    val mode = result.mode
                    val events = result.events

                    fun populateChart(
                        chart: BarChart,
                        yValueGetter: (GetRecords.TimelineEvent) -> Float
                    ) {
                        val values = mutableListOf<BarEntry>()
                        events.forEachIndexed { index, timelineEvent ->
                            values += BarEntry(
                                index.toFloat(),
                                yValueGetter.invoke(timelineEvent),
                                timelineEvent
                            )
                        }
                        val dataSet = BarDataSet(values, "")
                        dataSet.applyCommonSettings()
                        val dataSets = BarData(dataSet)

                        chart.run {
                            val format = when (mode) {
                                GetRecords.TimelineResult.MODE_DAYS -> {
                                    DateFormat.getDateFormat(context)
                                }
                                GetRecords.TimelineResult.MODE_ONE_DAY -> {
                                    DateFormat.getTimeFormat(context)
                                }
                                else -> throw IllegalStateException("Wrong format $mode")
                            }
                            xAxis.valueFormatter = object : ValueFormatter() {
                                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                                    val index = value.toInt()
                                    return if (index in events.indices) {
                                        format.format(Date(events[index].timePoint))
                                    } else {
                                        ""
                                    }
                                }
                            }
                            fitScreen()
                            data = dataSets
                            highlightValue(null)
                            animateY(750, Easing.EaseInOutQuad)
                        }
                    }

                    populateChart(
                        chart = binding.chartRecordTime,
                        yValueGetter = { it.duration.toFloat() }
                    )
                    populateChart(
                        chart = binding.chartRecordCount,
                        yValueGetter = { it.count.toFloat() }
                    )
                }
                else -> Unit
            }
        })
    }

    private fun BarChart.applyCommonSettings(durationFormatter: (Long) -> String) {
        val textColorPrimary = requireContext().themeColor(android.R.attr.textColorPrimary)
        xAxis.run {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            labelRotationAngle = 30f
            isGranularityEnabled = true
            granularity = 1f
            textColor = textColorPrimary
            axisLineColor = textColorPrimary
        }
        axisLeft.run {
            axisMinimum = 0f
            setDrawGridLines(false)
            setLabelCount(5, true)

            valueFormatter = object : ValueFormatter() {
                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    return durationFormatter.invoke(value.toLong())
                }
            }

            textColor = textColorPrimary
            axisLineColor = textColorPrimary
        }

        val markerView = RecordTimelineMarker(context)
        markerView.chartView = this
        marker = markerView

        axisRight.isEnabled = false
        description.isEnabled = false
        legend.isEnabled = false
        isDoubleTapToZoomEnabled = false

        extraLeftOffset = dp(4)
        extraRightOffset = dp(8)
    }

    private fun BarDataSet.applyCommonSettings() {
        val dynamicTheme = newDynamicTheme
        val colorPrimary = dynamicTheme.colorPrimary
        color = colorPrimary
        setDrawValues(false)
        highLightColor = dynamicTheme.colorSecondary
    }
}
