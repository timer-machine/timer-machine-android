package xyz.aprildown.timer.app.settings.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.color.ColorChooserDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import dagger.hilt.android.AndroidEntryPoint
import xyz.aprildown.timer.app.base.data.FlavorData
import xyz.aprildown.timer.app.base.data.PreferenceData.appTheme
import xyz.aprildown.timer.app.base.data.PreferenceData.getTypeColor
import xyz.aprildown.timer.app.base.data.PreferenceData.saveTypeColor
import xyz.aprildown.timer.app.base.ui.FlavorUiInjector
import xyz.aprildown.timer.app.base.ui.FlavorUiInjectorQualifier
import xyz.aprildown.timer.app.base.ui.MainCallback
import xyz.aprildown.timer.app.base.utils.AppThemeUtils
import xyz.aprildown.timer.app.settings.R
import xyz.aprildown.timer.component.key.ListItemWithLayout
import xyz.aprildown.timer.domain.entities.StepType
import xyz.aprildown.timer.domain.utils.Constants
import xyz.aprildown.tools.anko.dip
import xyz.aprildown.tools.helper.color
import xyz.aprildown.tools.helper.toColorStateList
import java.util.Optional
import javax.inject.Inject
import com.mikepenz.materialize.R as RMaterialize
import xyz.aprildown.timer.app.base.R as RBase
import xyz.aprildown.timer.component.key.R as RComponentKey

@AndroidEntryPoint
class ThemeFragment : Fragment(),
    BooleanToggle.Callback,
    ThemeColor.Callback,
    StepColor.Callback,
    ColorChooserDialog.ColorCallback,
    CustomThemeDialog.Callback {

    @Inject
    @FlavorUiInjectorQualifier
    lateinit var flavorUiInjector: Optional<FlavorUiInjector>

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var flavorData: FlavorData

    private lateinit var mainCallback: MainCallback.ActivityCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainCallback = context as MainCallback.ActivityCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(inflater.context).apply {
        setHasFixedSize(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val list = view as RecyclerView

        list.run {
            val lm = LinearLayoutManager(context)
            layoutManager = lm

            val itemAdapter = ItemAdapter<GenericItem>()
            itemAdapter.add(getThemeItems(context))
            val fastAdapter = FastAdapter.with(itemAdapter)
            adapter = fastAdapter

            var targetSelection = RecyclerView.NO_POSITION
            for ((index, item) in itemAdapter.adapterItems.withIndex()) {
                if (item !is ThemeColor) continue
                if (item.using ||
                    (item.name == NAME_CUSTOM && targetSelection == RecyclerView.NO_POSITION)
                ) {
                    targetSelection = index
                    if (!item.using) {
                        item.using = true
                        fastAdapter.notifyItemChanged(index)
                    }
                    break
                }
            }
            if (targetSelection != RecyclerView.NO_POSITION) {
                lm.scrollToPositionWithOffset(targetSelection, dip(108))
            }
        }
    }

    private fun getThemeItems(context: Context): List<GenericItem> {
        val items = mutableListOf<IItem<*>>()

        val (currentPrimary, currentSecondary, status, enableNav) = context.appTheme

        items += BooleanToggle(
            BooleanToggle.ID_SAME_STATUS,
            context.getString(RBase.string.theme_light_status_bar),
            status,
            this
        )
        items += BooleanToggle(
            BooleanToggle.ID_ENABLE_NAV,
            context.getString(RBase.string.theme_theme_nav_bar),
            enableNav,
            this
        )

        items += Group(context.getString(RBase.string.theme_title))

        items += context.getExtraThemes().map { appThemeColor ->
            ThemeColor(
                name = appThemeColor.name,
                primary = appThemeColor.primaryColor,
                secondary = appThemeColor.secondaryColor,
                using = appThemeColor.primaryColor == currentPrimary &&
                    appThemeColor.secondaryColor == currentSecondary,
                isPremium = false,
                callback = this
            )
        }

        if (flavorData.supportAdvancedFeatures) {
            val showPremiumIndicator =
                !sharedPreferences.getBoolean(Constants.PREF_HAS_PRO, false)
            items += getAdvancedThemes().map { appThemeColor ->
                ThemeColor(
                    name = appThemeColor.name,
                    primary = appThemeColor.primaryColor,
                    secondary = appThemeColor.secondaryColor,
                    using = appThemeColor.primaryColor == currentPrimary &&
                        appThemeColor.secondaryColor == currentSecondary,
                    isPremium = showPremiumIndicator,
                    callback = this
                )
            }
        }

        items += Group(context.getString(RBase.string.theme_custom_title))

        items += ThemeColor(
            name = NAME_CUSTOM,
            primary = currentPrimary,
            secondary = currentSecondary,
            using = false,
            callback = this
        )

        items += Group(context.getString(RBase.string.theme_step_color_title))

        items += StepColor(
            context.getString(RBase.string.theme_step_color_normal),
            StepType.NORMAL,
            StepType.NORMAL.getTypeColor(context),
            this
        )
        items += StepColor(
            context.getString(RBase.string.theme_step_color_notifier),
            StepType.NOTIFIER,
            StepType.NOTIFIER.getTypeColor(context),
            this
        )
        items += StepColor(
            context.getString(RBase.string.theme_step_color_start),
            StepType.START,
            StepType.START.getTypeColor(context),
            this
        )
        items += StepColor(
            context.getString(RBase.string.theme_step_color_end),
            StepType.END,
            StepType.END.getTypeColor(context),
            this
        )

        return items
    }

    private fun changeTheme(@ColorInt p: Int, @ColorInt a: Int) {
        val context = requireContext()
        val newAppTheme = context.appTheme.copy(
            colorPrimary = p, colorSecondary = a
        )
        context.appTheme = newAppTheme
        AppThemeUtils.configAppTheme(context, newAppTheme)
        reload()
    }

    override fun onThemeChange(name: String, primary: Int, secondary: Int, isPremium: Boolean) {
        when {
            name == NAME_CUSTOM -> {
                CustomThemeDialog.newInstance(primary, secondary)
                    .show(childFragmentManager, "custom_theme_dialog")
            }
            isPremium -> {
                flavorUiInjector.orElse(null)?.useMoreTheme(
                    fragment = this,
                    onApply = {
                        changeTheme(primary, secondary)
                    }
                )
            }
            else -> {
                changeTheme(primary, secondary)
            }
        }
    }

    override fun onCustomThemePick(primary: Int, secondary: Int) {
        changeTheme(primary, secondary)
    }

    override fun onBooleanToggleChange(toggleType: Int, newValue: Boolean) {
        val context = requireContext()
        when (toggleType) {
            BooleanToggle.ID_SAME_STATUS -> {
                val newAppTheme = context.appTheme.copy(sameStatusBar = newValue)
                context.appTheme = newAppTheme
                AppThemeUtils.configAppTheme(context, newAppTheme)
                reload()
            }
            BooleanToggle.ID_ENABLE_NAV -> {
                val newAppTheme = context.appTheme.copy(enableNav = newValue)
                context.appTheme = newAppTheme
                AppThemeUtils.configAppTheme(context, newAppTheme)
                reload()
            }
        }
    }

    override fun onStepColorClick(pos: Int, type: StepType) {
        val context = requireContext()
        ColorChooserDialog.Builder(context, RBase.string.theme_step_color_dialog_title)
            .tag(pos.toString())
            .doneButton(android.R.string.ok)
            .cancelButton(android.R.string.cancel)
            .customButton(RBase.string.theme_pick_colors)
            .presetsButton(RBase.string.theme_pick_presets)
            .backButton(RBase.string.theme_pick_back)
            .allowUserColorInputAlpha(false)
            .show(childFragmentManager)
    }

    override fun onColorChooserDismissed(dialog: ColorChooserDialog) = Unit
    override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
        val adapter = (view as? RecyclerView)?.adapter as? FastAdapter<*> ?: return
        val pos = dialog.tag()?.toIntOrNull() ?: return
        if (pos in 0 until adapter.itemCount) {
            val item = adapter.getItem(pos) as? StepColor ?: return
            item.stepType.saveTypeColor(requireContext(), selectedColor)
            item.color = selectedColor
            adapter.notifyItemChanged(pos)
        }
    }

    private fun reload() {
        (requireActivity() as? MainCallback.ActivityCallback)?.restartWithDestination(RBase.id.dest_theme)
    }
}

private const val NAME_CUSTOM = "Custom"

private class BooleanToggle(
    private val toggleType: Int,
    private val title: String,
    private var value: Boolean,
    private val callback: Callback
) : AbstractItem<BooleanToggle.ViewHolder>() {

    interface Callback {
        fun onBooleanToggleChange(toggleType: Int, newValue: Boolean)
    }

    override val layoutRes: Int = RComponentKey.layout.layout_list_item_with_layout
    override val type: Int = 1
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            listItemWithLayout.run {
                // FastAdapter override itemView's onClickListener
                // So we have to override it again in the onBind.
                delegateClickToCheckableLayout()

                listItem.setPrimaryText(title)
                getLayoutView<CompoundButton>().run {
                    isChecked = value
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked != value) {
                            value = isChecked
                            callback.onBooleanToggleChange(toggleType, isChecked)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ID_SAME_STATUS = 1
        const val ID_ENABLE_NAV = 2
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listItemWithLayout: ListItemWithLayout = (view as ListItemWithLayout).apply {
            setLayoutRes(RComponentKey.layout.view_list_item_with_layout_switch)
        }
    }
}

private class Group(
    private val title: String
) : AbstractItem<Group.ViewHolder>() {
    override val layoutRes: Int = R.layout.item_theme_group
    override val type: Int = 2
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            titleView.text = title
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView = view.findViewById(R.id.textThemeItemTitle)
    }
}

private class ThemeColor(
    val name: String,
    @ColorInt val primary: Int,
    @ColorInt val secondary: Int,
    var using: Boolean,
    var isPremium: Boolean = false,
    private val callback: Callback
) : AbstractItem<ThemeColor.ViewHolder>() {

    interface Callback {
        fun onThemeChange(
            name: String,
            @ColorInt primary: Int,
            @ColorInt secondary: Int,
            isPremium: Boolean
        )
    }

    override val layoutRes: Int = R.layout.item_theme_color
    override val type: Int = 3
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            bar.setBackgroundColor(primary)
            title.text = name
            val colorOnPrimary = AppThemeUtils.calculateOnColor(primary)
            title.setTextColor(colorOnPrimary)
            if (isPremium) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    title,
                    RBase.drawable.settings_premium, 0, 0, 0
                )
                TextViewCompat.setCompoundDrawableTintList(title, colorOnPrimary.toColorStateList())
            } else {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    title, 0, 0, 0, 0
                )
            }

            fab.backgroundTintList = ColorStateList.valueOf(secondary)
            fab.setImageResource(if (using) RBase.drawable.ic_check else 0)
            card.setOnClickListener {
                callback.onThemeChange(name, primary, secondary, isPremium)
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: View = view.findViewById(R.id.cardThemeItem)
        val bar: View = view.findViewById(R.id.frameThemeItemBar)
        val title: TextView = view.findViewById(R.id.textThemeItemTitle)
        val fab: FloatingActionButton = view.findViewById(R.id.fabThemeItem)
    }
}

private class StepColor(
    private val name: String,
    val stepType: StepType,
    @ColorInt var color: Int,
    private val callback: Callback
) : AbstractItem<StepColor.ViewHolder>() {

    interface Callback {
        fun onStepColorClick(pos: Int, type: StepType)
    }

    override val layoutRes: Int = RComponentKey.layout.layout_list_item_with_layout
    override val type: Int = 4
    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)
        holder.run {
            listItemWithLayout.run {
                listItem.setPrimaryText(name)
                getLayoutView<ImageView>().setBackgroundColor(color)
                setOnClickListener {
                    callback.onStepColorClick(bindingAdapterPosition, stepType)
                }
            }
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val listItemWithLayout: ListItemWithLayout = (view as ListItemWithLayout).apply {
            setLayoutRes(R.layout.widget_color_swatch)
        }
    }
}

private data class AppThemeColor(
    val name: String,
    @ColorInt val primaryColor: Int,
    @ColorInt val secondaryColor: Int
)

private fun Context.getExtraThemes(): List<AppThemeColor> {
    val list = mutableListOf<AppThemeColor>()

    fun Int.color() = color(this)

    list += AppThemeColor(
        "Original",
        RBase.color.colorPrimary.color(),
        RBase.color.colorSecondary.color()
    )
    list += AppThemeColor(
        "Deep Purple",
        RMaterialize.color.md_deep_purple_500.color(),
        RMaterialize.color.md_lime_800.color()
    )
    list += AppThemeColor(
        name = "Red",
        primaryColor = RMaterialize.color.md_red_500.color(),
        secondaryColor = RMaterialize.color.md_blue_500.color()
    )
    list += AppThemeColor(
        "Amber",
        RMaterialize.color.md_amber_500.color(),
        RMaterialize.color.md_deep_purple_400.color()
    )
    list += AppThemeColor(
        "Lime",
        RMaterialize.color.md_lime_500.color(),
        RMaterialize.color.md_purple_400.color()
    )

    return list
}

private fun getAdvancedThemes(): List<AppThemeColor> {
    val list = mutableListOf<AppThemeColor>()
    list += AppThemeColor(
        "Abyss Green",
        Color.parseColor("#2A9D8F"),
        Color.parseColor("#E9C46A")
    )
    list += AppThemeColor(
        "Lipstick Red",
        Color.parseColor("#E63946"),
        Color.parseColor("#457B9D")
    )
    list += AppThemeColor(
        "Chinese Violet",
        Color.parseColor("#6D597A"),
        Color.parseColor("#EAAC8B")
    )
    list += AppThemeColor(
        "Black Coral",
        Color.parseColor("#495867"),
        Color.parseColor("#FE5F55")
    )
    list += AppThemeColor(
        "Chrome Orange",
        Color.parseColor("#F6BD60"),
        Color.parseColor("#40916C")
    )
    list += AppThemeColor(
        "Middle Blue Green",
        Color.parseColor("#7DCFB6"),
        Color.parseColor("#F79256")
    )
    return list
}
