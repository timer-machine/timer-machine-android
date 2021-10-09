package xyz.aprildown.timer.app.timer.one.layout

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import xyz.aprildown.timer.app.base.data.PreferenceData.oneLayout
import xyz.aprildown.timer.app.timer.one.OneActivityInterface
import xyz.aprildown.timer.app.timer.one.R
import xyz.aprildown.timer.app.timer.one.databinding.FragmentOneLayoutBinding
import xyz.aprildown.timer.app.timer.one.layout.one.OneLayoutOneFragment
import xyz.aprildown.tools.helper.scale
import xyz.aprildown.tools.helper.setScale
import xyz.aprildown.tools.helper.triggerRipple
import xyz.aprildown.timer.app.base.R as RBase

class OneLayoutFragment : Fragment(R.layout.fragment_one_layout) {

    interface ChildFragment {
        fun provideEditDialogView(): View
    }

    private var currentUsingPosition = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (context as? OneActivityInterface)
            ?.setToolbarTitle(context.getString(RBase.string.one_layout_edit))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = view.context
        val binding = FragmentOneLayoutBinding.bind(view)

        val oneLayouts = listOf("one")
        val oneLayoutName = context.oneLayout
        currentUsingPosition = oneLayouts.indexOfFirst { it == oneLayoutName }
            .let { if (it == -1) 0 else it }

        val pagerAdapter = object : FragmentStateAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle
        ) {
            override fun getItemCount(): Int = 1
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> OneLayoutOneFragment()
                else -> throw IllegalStateException("Too big position $position")
            }
        }
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.setCurrentItem(currentUsingPosition, false)

        binding.fab.setOnClickListener {
            val childFragment = childFragmentManager.findFragmentByTag(
                "f${pagerAdapter.getItemId(binding.viewPager.currentItem)}"
            ) as? ChildFragment ?: return@setOnClickListener

            binding.fab.hide()
            BottomSheetDialog(context).apply {
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                setContentView(childFragment.provideEditDialogView())
                setOnDismissListener {
                    binding.fab.show()
                }
                window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }.show()
        }
        binding.fab.setScale(0f)
        binding.fab.post {
            binding.fab.animate().scale(1f)
                .withEndAction { binding.fab.triggerRipple() }
                .start()
        }

        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentUsingPosition = binding.viewPager.currentItem
                    context.oneLayout = oneLayouts[currentUsingPosition]
                }
            }
        )
    }
}
