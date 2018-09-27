package com.pandulapeter.campfire.feature.main.options.about

import android.animation.ObjectAnimator
import android.arch.lifecycle.Observer
import android.os.Bundle
import android.util.Property
import android.view.View
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentOptionsAboutBinding
import com.pandulapeter.campfire.feature.shared.CampfireFragment
import org.koin.android.viewmodel.ext.android.viewModel

class AboutFragment : CampfireFragment<FragmentOptionsAboutBinding, AboutViewModel>(R.layout.fragment_options_about) {

    override val viewModel by viewModel<AboutViewModel>()
    private val scale by lazy {
        object : Property<View, Float>(Float::class.java, "scale") {

            override fun set(view: View?, value: Float) {
                view?.run {
                    view.scaleX = value
                    view.scaleY = value
                }
            }

            override fun get(view: View) = view.scaleX
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.isUiBlocked = { getCampfireActivity().isUiBlocked }
        viewModel.apply {
            shouldShowErrorShowSnackbar.observe(viewLifecycleOwner, Observer { showSnackbar(R.string.options_about_error) })
            shouldShowWorkInProgressSnackbar.observe(viewLifecycleOwner, Observer { showSnackbar(R.string.options_about_no_in_app_purchase) })
            shouldShowNoEasterEggSnackbar.observe(viewLifecycleOwner, Observer {
                ObjectAnimator
                    .ofFloat(binding.logo, scale, 1f, 1.5f, 0.5f, 1.25f, 0.75f, 1.1f, 0.9f, 1f)
                    .setDuration(800)
                    .start()
            })
            shouldBlockUi.observe(viewLifecycleOwner, Observer { getCampfireActivity().isUiBlocked = true })
        }
    }
}