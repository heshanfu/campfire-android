package com.pandulapeter.campfire.feature.main.options

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.view.LayoutInflater
import android.view.View
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentOptionsBinding
import com.pandulapeter.campfire.databinding.ViewOptionsTabsBinding
import com.pandulapeter.campfire.feature.shared.TopLevelFragment
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.BundleArgumentDelegate
import com.pandulapeter.campfire.util.addPageScrollListener
import com.pandulapeter.campfire.util.withArguments
import org.koin.android.viewmodel.ext.android.viewModel

class OptionsFragment : TopLevelFragment<FragmentOptionsBinding, OptionsViewModel>(R.layout.fragment_options) {

    companion object {
        private var Bundle.shouldOpenChangelog by BundleArgumentDelegate.Boolean("shouldOpenChangelog")

        fun newInstance(shouldOpenChangelog: Boolean) = OptionsFragment().withArguments { it.shouldOpenChangelog = shouldOpenChangelog }
    }

    override val viewModel by viewModel<OptionsViewModel>()
    override val appBarView: TabLayout by lazy {
        DataBindingUtil.inflate<ViewOptionsTabsBinding>(
            LayoutInflater.from(getCampfireActivity().toolbarContext), R.layout.view_options_tabs, null, false
        ).tabLayout.apply {
            setupWithViewPager(binding.viewPager)
        }
    }
    private val pagerAdapter by lazy { OptionsFragmentPagerAdapter(getCampfireActivity(), childFragmentManager) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultToolbar.updateToolbarTitle(R.string.main_options)
        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 2
            addPageScrollListener(onPageSelected = {
                analyticsManager.run {
                    when (it) {
                        0 -> onOptionsScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_OPTIONS_PREFERENCES)
                        1 -> onOptionsScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_OPTIONS_WHAT_IS_NEW)
                        2 -> onOptionsScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_OPTIONS_ABOUT)
                    }
                }
            })
            if (arguments?.shouldOpenChangelog == true) {
                currentItem = 1
            } else {
                analyticsManager.onOptionsScreenOpened(AnalyticsManager.PARAM_VALUE_SCREEN_OPTIONS_PREFERENCES)
            }
        }
    }

    fun navigateToChangelog() {
        binding.viewPager.currentItem = 1
    }
}