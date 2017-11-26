package com.pandulapeter.campfire.ioc

import com.pandulapeter.campfire.feature.detail.DetailActivity
import com.pandulapeter.campfire.feature.home.HomeActivity
import com.pandulapeter.campfire.feature.home.downloaded.DownloadedFragment
import com.pandulapeter.campfire.feature.home.favorites.FavoritesFragment
import com.pandulapeter.campfire.feature.home.cloud.CloudFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class CampfireModule {

    @ContributesAndroidInjector
    abstract fun contributeHomeActivity(): HomeActivity

    @ContributesAndroidInjector
    abstract fun contributeDetailActivity(): DetailActivity

    @ContributesAndroidInjector
    abstract fun contributeCloudFragment(): CloudFragment

    @ContributesAndroidInjector
    abstract fun contributeDownloadedFragment(): DownloadedFragment

    @ContributesAndroidInjector
    abstract fun contributeFavoritesFragment(): FavoritesFragment
}