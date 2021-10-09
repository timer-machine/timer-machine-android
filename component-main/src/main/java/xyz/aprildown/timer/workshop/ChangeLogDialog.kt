package xyz.aprildown.timer.workshop

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.ArrayRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import xyz.aprildown.timer.app.base.utils.openWebsiteWithWarning
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.timer.workshop.reminder.AppReminder
import xyz.aprildown.tools.anko.toast
import xyz.aprildown.tools.helper.IntentHelper
import xyz.aprildown.tools.helper.setTextIfChanged
import xyz.aprildown.tools.helper.startActivitySafely
import xyz.aprildown.timer.app.base.R as RBase

class ChangeLogDialog(private val context: Context) {
    fun show() {
        MaterialAlertDialogBuilder(context)
            .setTitle(RBase.string.update_title)
            .setAdapter(
                ChangeLogAdapter(
                    generateEntries(
                        RBase.array.update_content_600,
                        RBase.array.update_content_580,
                        RBase.array.update_content_570,
                        RBase.array.update_content_560,
                        RBase.array.update_content_550,
                        RBase.array.update_content_541,
                        RBase.array.update_content_540,
                        RBase.array.update_content_530,
                        RBase.array.update_content_520,
                        RBase.array.update_content_510,
                        RBase.array.update_content_500,
                    ),
                    onMoreClick = {
                        context.openWebsiteWithWarning(Constants.getChangeLogLink(context))
                    }
                ),
                null
            )
            .setPositiveButton(android.R.string.ok, null)
            .setNeutralButton(RBase.string.rate_five_stars) { _, _ ->
                AppReminder(context, Constants.REMINDER_RATE).ok()
                context.toast(RBase.string.thanks)
                context.startActivitySafely(
                    IntentHelper.appStorePage(context),
                    wrongMessageRes = RBase.string.no_action_found
                )
            }
            .show()
    }

    private fun generateEntries(@ArrayRes vararg content: Int): List<ChangeLogAdapter.Entry> {
        val result = mutableListOf<ChangeLogAdapter.Entry>()
        val res = context.resources
        content.forEach { contentRes ->
            val strings = res.getTextArray(contentRes)
            result += ChangeLogAdapter.Entry.VersionTitle(strings[0])
            for (i in 1 until strings.size) {
                result += ChangeLogAdapter.Entry.VersionContent(strings[i])
            }
        }
        result += ChangeLogAdapter.Entry.More
        return result
    }
}

private class ChangeLogAdapter(
    private val entries: List<Entry>,
    private val onMoreClick: () -> Unit
) : BaseAdapter() {

    sealed class Entry {
        data class VersionTitle(val title: CharSequence) : Entry()
        data class VersionContent(val content: CharSequence) : Entry()
        object More : Entry()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val context = parent.context
        val viewType = getItemViewType(position)
        val view = convertView ?: View.inflate(
            context,
            when (viewType) {
                VIEW_TYPE_TITLE -> R.layout.item_change_log_title
                VIEW_TYPE_CONTENT -> R.layout.item_change_log_content
                else -> R.layout.item_change_log_more
            },
            null
        )
        val targetView = view.findViewById<TextView>(R.id.text)
        if (viewType == VIEW_TYPE_MORE) {
            targetView.setOnClickListener {
                onMoreClick.invoke()
            }
        } else {
            targetView.setOnClickListener(null)
        }
        targetView.setTextIfChanged(
            when (val entry = getItem(position)) {
                is Entry.VersionTitle -> entry.title
                is Entry.VersionContent -> entry.content
                is Entry.More -> context.getString(RBase.string.update_more)
            }
        )
        return view
    }

    override fun getCount(): Int = entries.size
    override fun getItem(position: Int): Entry = entries[position]
    override fun getItemId(position: Int): Long = position.toLong()
    override fun getViewTypeCount(): Int = 3
    override fun getItemViewType(position: Int): Int = when (entries[position]) {
        is Entry.VersionTitle -> VIEW_TYPE_TITLE
        is Entry.VersionContent -> VIEW_TYPE_CONTENT
        is Entry.More -> VIEW_TYPE_MORE
    }
}

private const val VIEW_TYPE_TITLE = 0
private const val VIEW_TYPE_CONTENT = 1
private const val VIEW_TYPE_MORE = 2
