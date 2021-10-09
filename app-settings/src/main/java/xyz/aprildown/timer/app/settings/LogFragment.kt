package xyz.aprildown.timer.app.settings

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.tools.helper.scrollToBottom
import java.io.File
import xyz.aprildown.timer.app.base.R as RBase

class LogFragment : Fragment(R.layout.fragment_log) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val textView = view.findViewById<TextView>(R.id.textLogBody)
        val logFile = File(context.filesDir, Constants.FILENAME_RUNNING_LOG)
        if (logFile.exists()) {
            textView.text = logFile.readText()
        } else {
            textView.setText(RBase.string.empty)
        }

        view.findViewById<ScrollView>(R.id.scrollLogRoot).scrollToBottom()
    }
}
