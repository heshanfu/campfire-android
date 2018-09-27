package com.pandulapeter.campfire.feature.main.home.onboarding.page.songAppearance

import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentOnboardingSongAppearanceBinding
import com.pandulapeter.campfire.feature.main.home.onboarding.page.OnboardingPageFragment
import org.koin.android.viewmodel.ext.android.viewModel

class SongAppearanceFragment : OnboardingPageFragment<FragmentOnboardingSongAppearanceBinding, SongAppearanceViewModel>(R.layout.fragment_onboarding_song_appearance) {

    override val viewModel by viewModel<SongAppearanceViewModel>()

    override fun onResume() {
        super.onResume()
        viewModel.initialize()
    }
}