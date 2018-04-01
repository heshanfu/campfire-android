package com.pandulapeter.campfire.feature.detail

import android.content.Context
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.FragmentDetailBinding
import com.pandulapeter.campfire.feature.CampfireFragment
import com.pandulapeter.campfire.util.*

class DetailFragment : CampfireFragment<FragmentDetailBinding>(R.layout.fragment_detail) {
    override var onFloatingActionButtonClicked: (() -> Unit)? = {
        mainActivity.autoScrollControl.run {
            if (tag == null) {
                val drawable = if (visibleOrInvisible) drawablePauseToPlay else drawablePlayToPause
                mainActivity.floatingActionButton.setImageDrawable(drawable)
                animatedVisibilityEnd = !animatedVisibilityEnd
                drawable?.start()
            }
        }
    }
    override val navigationMenu = R.menu.detail
    private val drawablePlayToPause by lazy { context.animatedDrawable(R.drawable.avd_play_to_pause_24dp) }
    private val drawablePauseToPlay by lazy { context.animatedDrawable(R.drawable.avd_pause_to_play_24dp) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide(Gravity.BOTTOM)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        defaultToolbar.updateToolbarTitle("Title", "Subtitle")
        mainActivity.transformMainToolbarButton(true)
        mainActivity.floatingActionButton.run {
            setImageDrawable(context.drawable(R.drawable.ic_play_24dp))
            show()
        }
        mainActivity.autoScrollControl.visibleOrGone = false
    }

    override fun inflateToolbarButtons(context: Context) = listOf<View>(
        context.createToolbarButton(R.drawable.ic_song_options_24dp) { mainActivity.openSecondaryNavigationDrawer() }
    )
}