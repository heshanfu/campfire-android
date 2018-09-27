package com.pandulapeter.campfire.feature.main.options.changelog

import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.view.View
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentOptionsChangelogBinding
import com.pandulapeter.campfire.feature.shared.CampfireFragment
import org.koin.android.viewmodel.ext.android.viewModel

class ChangelogFragment : CampfireFragment<FragmentOptionsChangelogBinding, ChangelogViewModel>(R.layout.fragment_options_changelog) {

    override val viewModel by viewModel<ChangelogViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.itemAnimator = object : DefaultItemAnimator() {
            init {
                supportsChangeAnimations = false
            }
        }
    }
}