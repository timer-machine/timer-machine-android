package xyz.aprildown.timer.app.timer.list.record

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import xyz.aprildown.timer.app.timer.list.R

internal class RecordTimelineMarker(
    context: Context
) : MarkerView(context, R.layout.view_record_timeline_marker) {

    private val titleView: TextView = findViewById(R.id.textRecordMarkerTitle)
    private val contentView: TextView = findViewById(R.id.textRecordChartContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {

        if (e != null) {
            titleView.text = chartView?.xAxis?.valueFormatter?.getAxisLabel(e.x, null)
            contentView.text = (chartView as? BarChart)?.axisLeft?.valueFormatter
                ?.getAxisLabel(e.y, null)
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() * 1.25f)
    }
}
