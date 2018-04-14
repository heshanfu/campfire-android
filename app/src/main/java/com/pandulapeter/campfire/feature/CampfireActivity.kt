package com.pandulapeter.campfire.feature

import android.animation.Animator
import android.animation.LayoutTransition
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.MenuRes
import android.support.design.internal.NavigationMenuView
import android.support.design.widget.AppBarLayout
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.ViewCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.transition.Explode
import android.transition.Transition
import android.view.Gravity
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import com.pandulapeter.campfire.BuildConfig
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.Playlist
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.databinding.ActivityCampfireBinding
import com.pandulapeter.campfire.feature.detail.DetailFragment
import com.pandulapeter.campfire.feature.home.history.HistoryFragment
import com.pandulapeter.campfire.feature.home.library.LibraryFragment
import com.pandulapeter.campfire.feature.home.manageDownloads.ManageDownloadsFragment
import com.pandulapeter.campfire.feature.home.managePlaylists.ManagePlaylistsFragment
import com.pandulapeter.campfire.feature.home.options.OptionsFragment
import com.pandulapeter.campfire.feature.home.playlist.PlaylistFragment
import com.pandulapeter.campfire.feature.shared.TopLevelFragment
import com.pandulapeter.campfire.feature.shared.dialog.AlertDialogFragment
import com.pandulapeter.campfire.feature.shared.dialog.NewPlaylistDialogFragment
import com.pandulapeter.campfire.integration.AppShortcutManager
import com.pandulapeter.campfire.util.*
import org.koin.android.ext.android.inject

class CampfireActivity : AppCompatActivity(), AlertDialogFragment.OnDialogItemsSelectedListener, PlaylistRepository.Subscriber {

    companion object {
        private const val DIALOG_ID_EXIT_CONFIRMATION = 1
        private const val DIALOG_ID_PRIVACY_POLICY = 2
        const val SCREEN_LIBRARY = "library"
        const val SCREEN_HISTORY = "history"
        const val SCREEN_OPTIONS = "options"
        const val SCREEN_MANAGE_PLAYLISTS = "managePlaylists"
        const val SCREEN_MANAGE_DOWNLOADS = "manageDownloads"

        private var Intent.screenToOpen by IntentExtraDelegate.String("screenToOpen")

        fun getLibraryIntent(context: Context) = Intent(context, CampfireActivity::class.java).apply {
            screenToOpen = SCREEN_LIBRARY
        }

        fun getPlaylistIntent(context: Context, playlistId: String) = Intent(context, CampfireActivity::class.java).apply {
            screenToOpen = playlistId
        }
    }

    private var Bundle.isOnDetailScreen by BundleArgumentDelegate.Boolean("isOnDetailScreen")
    private var Bundle.currentScreenId by BundleArgumentDelegate.Int("currentScreenId")
    private var Bundle.currentPlaylistId by BundleArgumentDelegate.String("currentPlaylistId")
    private var Bundle.isAppBarExpanded by BundleArgumentDelegate.Boolean("isAppBarExpanded")
    private var Bundle.toolbarContainerScrollFlags by BundleArgumentDelegate.Boolean("shouldAllowAppBarScrolling")
    private val binding by lazy { DataBindingUtil.setContentView<ActivityCampfireBinding>(this, R.layout.activity_campfire) }
    private val currentFragment get() = supportFragmentManager.findFragmentById(R.id.fragment_container) as? TopLevelFragment<*, *>?
    private val drawableMenuToBack by lazy { animatedDrawable(R.drawable.avd_menu_to_back_24dp) }
    private val drawableBackToMenu by lazy { animatedDrawable(R.drawable.avd_back_to_menu_24dp) }
    private val appShortcutManager by inject<AppShortcutManager>()
    private val preferenceDatabase by inject<PreferenceDatabase>()
    private val playlistRepository by inject<PlaylistRepository>()
    private var currentPlaylistId = ""
    private var currentScreenId = R.id.library
    private var forceExpandAppBar = true
    private val colorWhite by lazy { color(R.color.white) }
    private val playlistsContainerItem by lazy { binding.primaryNavigation.menu.findItem(R.id.playlists).subMenu }
    private val playlistIdMap = mutableMapOf<Int, String>()
    private var newPlaylistId = 0
    private var startTime = 0L
    val autoScrollControl get() = binding.autoScrollControl
    val toolbarContext get() = binding.appBarLayout.context!!
    val secondaryNavigationMenu get() = binding.secondaryNavigation.menu ?: throw IllegalStateException("The secondary navigation drawer has no menu inflated.")
    val snackbarRoot get() = binding.rootCoordinatorLayout
    var shouldAllowAppBarScrolling
        get() = (binding.toolbarContainer.layoutParams as AppBarLayout.LayoutParams).scrollFlags != 0
        set(value) {
            (binding.toolbarContainer.layoutParams as AppBarLayout.LayoutParams).scrollFlags = if (value) {
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            } else 0
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Set the theme and the task description.
        setTheme(if (preferenceDatabase.shouldUseDarkTheme) R.style.DarkTheme else R.style.LightTheme)
        @Suppress("ConstantConditionIf")
        setTaskDescription(
            ActivityManager.TaskDescription(
                getString(R.string.campfire) + if (BuildConfig.BUILD_TYPE == "release") "" else " (" + BuildConfig.BUILD_TYPE + ")",
                null, color(R.color.primary)
            )
        )
        super.onCreate(savedInstanceState)
        startTime = System.currentTimeMillis()

        // Initialize the app bar.
        val appBarElevation = dimension(R.dimen.toolbar_elevation).toFloat()
        binding.toolbarMainButton.setOnClickListener {
            if (currentFragment is DetailFragment) {
                supportFragmentManager.popBackStack()
                updateMainToolbarButton(false)
            } else {
                hideKeyboard(currentFocus)
                binding.drawerLayout.openDrawer(Gravity.START)
            }
        }
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, _ -> ViewCompat.setElevation(appBarLayout, appBarElevation) }
        shouldAllowAppBarScrolling = true

        // Initialize the drawer layout.
        binding.drawerLayout.addDrawerListener(
            onDrawerStateChanged = {
                if (it == DrawerLayout.STATE_DRAGGING) {
                    expandAppBar()
                }
                currentFragment?.onDrawerStateChanged(it)
                if (it == DrawerLayout.STATE_DRAGGING) {
                    hideKeyboard(currentFocus)
                }
            })

        // Initialize the primary side navigation drawer.
        binding.primaryNavigation.disableScrollbars()
        (binding.primaryNavigation.getHeaderView(0)?.findViewById<View>(R.id.version) as? TextView)?.text = getString(R.string.home_version_pattern, BuildConfig.VERSION_NAME)
        binding.primaryNavigation.setNavigationItemSelectedListener { menuItem ->
            if (currentScreenId == menuItem.itemId) {
                consumeAndCloseDrawers()
            } else {
                if (menuItem.itemId != newPlaylistId) {
                    currentScreenId = menuItem.itemId
                }
                return@setNavigationItemSelectedListener when (menuItem.itemId) {
                    R.id.library -> consumeAndCloseDrawers {
                        appShortcutManager.onLibraryOpened()
                        supportFragmentManager.handleReplace { LibraryFragment() }
                    }
                    R.id.history -> consumeAndCloseDrawers { supportFragmentManager.handleReplace { HistoryFragment() } }
                    R.id.options -> consumeAndCloseDrawers { supportFragmentManager.handleReplace { OptionsFragment() } }
                    R.id.manage_playlists -> consumeAndCloseDrawers { supportFragmentManager.handleReplace { ManagePlaylistsFragment() } }
                    R.id.manage_downloads -> consumeAndCloseDrawers { supportFragmentManager.handleReplace { ManageDownloadsFragment() } }
                    newPlaylistId -> {
                        currentFragment?.hideSnackbar()
                        NewPlaylistDialogFragment.show(supportFragmentManager)
                        binding.drawerLayout.closeDrawers()
                        false
                    }
                    else -> consumeAndCloseDrawers { playlistIdMap[menuItem.itemId]?.let { openPlaylistScreen(it) } }
                }
            }
        }

        // Initialize the secondary side navigation drawer.
        binding.secondaryNavigation.disableScrollbars()
        binding.secondaryNavigation.setNavigationItemSelectedListener { currentFragment?.onNavigationItemSelected(it) ?: false }

        // Initialize the floating action button.
        binding.floatingActionButton.setOnClickListener { currentFragment?.onFloatingActionButtonPressed() }

        // Restore instance state if possible.
        if (savedInstanceState == null) {
            handleNewIntent()
        } else {
            binding.toolbarMainButton.setImageDrawable(drawable(if (savedInstanceState.isOnDetailScreen) R.drawable.ic_back_24dp else R.drawable.ic_menu_24dp))
            currentScreenId = savedInstanceState.currentScreenId
            currentPlaylistId = savedInstanceState.currentPlaylistId
            shouldAllowAppBarScrolling = savedInstanceState.toolbarContainerScrollFlags
            if (currentScreenId == R.id.options) {
                forceExpandAppBar = savedInstanceState.isAppBarExpanded
            }
        }
        binding.drawerLayout.setDrawerLockMode(if (currentFragment is DetailFragment) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.START)

        // Show the privacy consent dialog if needed.
        if (preferenceDatabase.shouldShowPrivacyPolicy) {
            AlertDialogFragment.show(
                id = DIALOG_ID_PRIVACY_POLICY,
                fragmentManager = supportFragmentManager,
                title = R.string.home_privacy_policy_title,
                message = R.string.home_privacy_policy_message,
                positiveButton = R.string.home_privacy_policy_positive,
                negativeButton = R.string.home_privacy_policy_negative,
                cancelable = false
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent
        handleNewIntent()
    }

    override fun onResume() {
        super.onResume()
        playlistRepository.subscribe(this)
        if (currentFocus is EditText) {
            binding.drawerLayout.run { post { closeDrawers() } }
        }
    }

    override fun onPause() {
        super.onPause()
        playlistRepository.unsubscribe(this)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(Gravity.START)) {
            binding.drawerLayout.closeDrawer(Gravity.START)
        } else {
            if (binding.drawerLayout.isDrawerOpen(Gravity.END)) {
                binding.drawerLayout.closeDrawer(Gravity.END)
            } else {
                val fragment = currentFragment
                if (fragment == null || !fragment.onBackPressed()) {
                    if (fragment is DetailFragment) {
                        updateMainToolbarButton(false)
                        super.onBackPressed()
                    } else {
                        if (preferenceDatabase.shouldShowExitConfirmation) {
                            AlertDialogFragment.show(
                                id = DIALOG_ID_EXIT_CONFIRMATION,
                                fragmentManager = supportFragmentManager,
                                title = R.string.home_exit_confirmation_title,
                                message = R.string.home_exit_confirmation_message,
                                positiveButton = R.string.home_exit_confirmation_close,
                                negativeButton = R.string.cancel
                            )
                        } else {
                            onPositiveButtonSelected(DIALOG_ID_EXIT_CONFIRMATION)
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.isOnDetailScreen = currentFragment is DetailFragment
        outState?.currentScreenId = currentScreenId
        outState?.isAppBarExpanded = binding.appBarLayout.height - binding.appBarLayout.bottom == 0
        outState?.toolbarContainerScrollFlags = shouldAllowAppBarScrolling
        outState?.currentPlaylistId = playlistIdMap[currentScreenId] ?: ""
    }

    override fun onPositiveButtonSelected(id: Int) {
        when (id) {
            DIALOG_ID_EXIT_CONFIRMATION -> supportFinishAfterTransition()
            DIALOG_ID_PRIVACY_POLICY -> {
                preferenceDatabase.shouldShowPrivacyPolicy = false
                preferenceDatabase.shouldShareUsageData = true
            }
        }
    }

    override fun onNegativeButtonSelected(id: Int) {
        if (id == DIALOG_ID_PRIVACY_POLICY) {
            preferenceDatabase.shouldShowPrivacyPolicy = false
        }
    }

    override fun onPlaylistsUpdated(playlists: List<Playlist>) = updatePlaylists(playlists)

    override fun onPlaylistOrderChanged(playlists: List<Playlist>) = updatePlaylists(playlists)

    fun addViewToAppBar(view: View, immediately: Boolean) {
        binding.appBarLayout.run {
            if (immediately || System.currentTimeMillis() - startTime < 300) {
                layoutTransition = null
                while (childCount > 1) {
                    getChildAt(1).run { removeView(this) }
                }
                post { toggleTransitionMode(true) }
                addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            } else {
                postDelayed({
                    while (childCount > 1) {
                        getChildAt(1).run { removeView(this) }
                    }
                    toggleTransitionMode(true)
                    addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }, 120)
            }
        }
    }

    private fun removeViewFromAppBar() {
        binding.appBarLayout.run {
            if (childCount > 1) {
                getChildAt(1).run {
                    postDelayed({
                        if (isAttachedToWindow) {
                            layoutTransition = LayoutTransition()
                            removeView(this)
                        }
                    }, 100)
                }
            }
        }
    }

    fun expandAppBar() {
        binding.appBarLayout.setExpanded(forceExpandAppBar, forceExpandAppBar)
        forceExpandAppBar = true
    }

    fun updateMainToolbarButton(shouldShowBackButton: Boolean) {
        fun changeDrawable() = binding.toolbarMainButton.setImageDrawable((if (shouldShowBackButton) drawableMenuToBack else drawableBackToMenu).apply { this?.start() })
        if (shouldShowBackButton) {
            changeDrawable()
        } else {
            binding.toolbarMainButton.postDelayed({ changeDrawable() }, 100)
        }
        binding.drawerLayout.setDrawerLockMode(if (shouldShowBackButton) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.START)
    }

    fun updateToolbarTitleView(toolbar: View, width: Int = 0) {
        val oldView = binding.toolbarTitleContainer.run {
            while (childCount > 1) {
                removeViewAt(1)
            }
            if (childCount > 0) getChildAt(0) else null
        }
        binding.toolbarTitleContainer.addView(
            toolbar.apply { visibleOrGone = oldView?.id == R.id.default_toolbar },
            FrameLayout.LayoutParams(if (width == 0) ViewGroup.LayoutParams.MATCH_PARENT else width, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            })
        oldView?.run {
            visibleOrGone = false
            postOnAnimation { binding.toolbarTitleContainer.removeView(this) }
        }
        toolbar.run { postOnAnimation { visibleOrGone = true } }
    }

    fun updateToolbarButtons(buttons: List<View>) = binding.toolbarButtonContainer.run {
        if (childCount == 0) {
            buttons.forEach { addView(it, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        }
    }

    fun enableSecondaryNavigationDrawer(@MenuRes menuResourceId: Int) {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.END)
        binding.secondaryNavigation.menu.clear()
        binding.secondaryNavigation.inflateMenu(menuResourceId)
    }

    fun openSecondaryNavigationDrawer() {
        hideKeyboard(currentFocus)
        binding.drawerLayout.openDrawer(Gravity.END)
    }

    fun closeSecondaryNavigationDrawer() = binding.drawerLayout.closeDrawer(Gravity.END)

    fun enableFloatingActionButton() = binding.floatingActionButton.show()

    fun disableFloatingActionButton() = binding.autoScrollControl.run {
        if (animatedVisibilityEnd) {
            animatedVisibilityEnd = false
            (tag as? Animator)?.let {
                it.addListener(onAnimationEnd = {
                    binding.floatingActionButton.hide()
                    tag = null
                    visibleOrGone = false

                })
            }
        } else {
            binding.floatingActionButton.hide()
        }
    }

    fun updateFloatingActionButtonDrawable(drawable: Drawable?) = binding.floatingActionButton.setImageDrawable(drawable.apply { this?.setTint(colorWhite) })

    fun beforeScreenChanged() {

        // Hide the keyboard.
        hideKeyboard(currentFocus)

        // Reset the app bar.
        toggleTransitionMode(false)
        shouldAllowAppBarScrolling = true
        binding.toolbarButtonContainer.removeAllViews()
        removeViewFromAppBar()
        expandAppBar()

        // Reset the secondary navigation drawer.
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.END)
        binding.secondaryNavigation.menu.clear()

        // Reset the floating action button.
        disableFloatingActionButton()
    }

    fun toggleTransitionMode(boolean: Boolean) {
        if (boolean) {
            binding.appBarLayout.layoutTransition = LayoutTransition().apply {
                setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0)
            }
            binding.coordinatorLayout.layoutTransition = LayoutTransition().apply {
                enableTransitionType(LayoutTransition.CHANGING)
            }
        } else {
            binding.appBarLayout.layoutTransition = LayoutTransition().apply {
                disableTransitionType(LayoutTransition.DISAPPEARING)
                enableTransitionType(LayoutTransition.CHANGING)

            }
            binding.coordinatorLayout.layoutTransition = LayoutTransition().apply {
                disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
            }
        }
    }

    private fun handleNewIntent() {
        if (currentFragment is DetailFragment) {
            if (intent.screenToOpen.isEmpty()) {
                return
            } else {
                supportFragmentManager.popBackStackImmediate()
                updateMainToolbarButton(false)
            }
        }
        when (intent.screenToOpen) {
            "" -> preferenceDatabase.lastScreen.let {
                when (it) {
                    "" -> openLibraryScreen()
                    SCREEN_LIBRARY -> openLibraryScreen()
                    SCREEN_HISTORY -> openHistoryScreen()
                    SCREEN_OPTIONS -> openOptionsScreen()
                    SCREEN_MANAGE_PLAYLISTS -> openManagePlaylistsScreen()
                    SCREEN_MANAGE_DOWNLOADS -> openManageDownloadsScreen()
                    else -> openPlaylistScreen(it)
                }
            }
            SCREEN_LIBRARY -> openLibraryScreen()
            else -> openPlaylistScreen(intent.screenToOpen)
        }
    }

    fun openLibraryScreen() {
        if (currentFragment !is LibraryFragment) {
            supportFragmentManager.handleReplace { LibraryFragment() }
            currentScreenId = R.id.library
            binding.primaryNavigation.setCheckedItem(R.id.library)
            appShortcutManager.onLibraryOpened()
        }
    }

    private fun openHistoryScreen() {
        if (currentFragment !is HistoryFragment) {
            supportFragmentManager.handleReplace { HistoryFragment() }
            currentScreenId = R.id.history
            binding.primaryNavigation.setCheckedItem(R.id.history)
        }
    }

    private fun openOptionsScreen() {
        if (currentFragment !is OptionsFragment) {
            supportFragmentManager.handleReplace { OptionsFragment() }
            currentScreenId = R.id.options
            binding.primaryNavigation.setCheckedItem(R.id.options)
        }
    }

    fun openPlaylistScreen(playlistId: String) {
        if (currentFragment !is PlaylistFragment || currentPlaylistId != playlistId) {
            playlistIdMap.forEach {
                if (it.value == playlistId) {
                    currentScreenId = it.key
                    binding.primaryNavigation.setCheckedItem(it.key)
                }
            }
            beforeScreenChanged()
            currentPlaylistId = playlistId
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PlaylistFragment.newInstance(playlistId))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
            appShortcutManager.onPlaylistOpened(playlistId)
        }
    }

    private fun openManagePlaylistsScreen() {
        if (currentFragment !is ManagePlaylistsFragment) {
            currentScreenId = R.id.manage_playlists
            supportFragmentManager.handleReplace { ManagePlaylistsFragment() }
            binding.primaryNavigation.setCheckedItem(R.id.manage_playlists)
        }
    }

    private fun openManageDownloadsScreen() {
        if (currentFragment !is ManageDownloadsFragment) {
            currentScreenId = R.id.manage_downloads
            supportFragmentManager.handleReplace { ManageDownloadsFragment() }
            binding.primaryNavigation.setCheckedItem(R.id.manage_downloads)
        }
    }

    fun openDetailScreen(clickedView: View, songs: List<Song>, shouldExplode: Boolean, index: Int = 0, shouldShowManagePlaylist: Boolean = true) {
        fun createTransition(delay: Long) = Explode().apply {
            propagation = null
            epicenterCallback = object : Transition.EpicenterCallback() {
                override fun onGetEpicenter(transition: Transition?) = Rect().apply { clickedView.getGlobalVisibleRect(this) }
            }
            startDelay = delay
        }
        currentFragment?.run {
            if (shouldExplode) {
                exitTransition = createTransition(0)
                reenterTransition = createTransition(DetailFragment.TRANSITION_DELAY)
            } else {
                exitTransition = null
                reenterTransition = null
            }
        }
        supportFragmentManager.beginTransaction()
            .setAllowOptimization(true)
            .replace(R.id.fragment_container, DetailFragment.newInstance(songs, index, shouldShowManagePlaylist))
            .addSharedElement(clickedView, clickedView.transitionName)
            .addToBackStack(null)
            .commit()
    }

    private fun updatePlaylists(playlists: List<Playlist>) {
        val isLookingForUpdatedId = currentFragment is PlaylistFragment
        playlistsContainerItem.run {
            clear()
            playlistIdMap.clear()
            playlists
                .sortedBy { it.order }
                .filter { it.id != playlistRepository.hiddenPlaylistId }
                .forEachIndexed { index, playlist ->
                    val id = View.generateViewId()
                    playlistIdMap[id] = playlist.id
                    if (isLookingForUpdatedId && playlist.id == currentPlaylistId) {
                        currentScreenId = id
                        binding.primaryNavigation.run { post { setCheckedItem(id) } }
                    }
                    addPlaylistItem(index, id, playlist.title ?: getString(R.string.home_favorites))
                }
            if (playlists.size < Playlist.MAXIMUM_PLAYLIST_COUNT) {
                newPlaylistId = View.generateViewId()
                addPlaylistItem(playlists.size, newPlaylistId, getString(R.string.home_new_playlist), true)
            }
            setGroupCheckable(R.id.playlist_container, true, true)
            appShortcutManager.updateAppShortcuts()
        }
    }

    private fun SubMenu.addPlaylistItem(index: Int, id: Int, title: String, shouldUseAddIcon: Boolean = false) =
        add(R.id.playlist_container, id, index, title).run {
            setIcon(if (shouldUseAddIcon) R.drawable.ic_new_playlist_24dp else R.drawable.ic_playlist_24dp)
        }

    private inline fun <reified T : TopLevelFragment<*, *>> FragmentManager.handleReplace(crossinline newInstance: () -> T) {
        currentFragment?.exitTransition = null
        beginTransaction()
            .replace(R.id.fragment_container, findFragmentByTag(T::class.java.name) ?: newInstance.invoke(), T::class.java.name)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private inline fun consumeAndCloseDrawers(crossinline action: () -> Unit = {}) = consume {
        action()
        binding.drawerLayout.closeDrawers()
    }

    private fun NavigationView.disableScrollbars() {
        (getChildAt(0) as? NavigationMenuView)?.isVerticalScrollBarEnabled = false
    }
}