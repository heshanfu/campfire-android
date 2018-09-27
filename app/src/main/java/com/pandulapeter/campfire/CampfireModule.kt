package com.pandulapeter.campfire

import android.arch.persistence.room.Room
import com.google.gson.GsonBuilder
import com.pandulapeter.campfire.data.networking.NetworkManager
import com.pandulapeter.campfire.data.persistence.Database
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.*
import com.pandulapeter.campfire.feature.detail.DetailEventBus
import com.pandulapeter.campfire.feature.detail.DetailPageEventBus
import com.pandulapeter.campfire.feature.detail.DetailViewModel
import com.pandulapeter.campfire.feature.detail.page.DetailPageViewModel
import com.pandulapeter.campfire.feature.detail.page.parsing.SongParser
import com.pandulapeter.campfire.feature.main.collections.CollectionsViewModel
import com.pandulapeter.campfire.feature.main.collections.detail.CollectionDetailViewModel
import com.pandulapeter.campfire.feature.main.history.HistoryViewModel
import com.pandulapeter.campfire.feature.main.home.HomeContainerViewModel
import com.pandulapeter.campfire.feature.main.home.home.HomeViewModel
import com.pandulapeter.campfire.feature.main.home.onboarding.OnboardingViewModel
import com.pandulapeter.campfire.feature.main.home.onboarding.page.contentLanguage.ContentLanguageViewModel
import com.pandulapeter.campfire.feature.main.home.onboarding.page.songAppearance.SongAppearanceViewModel
import com.pandulapeter.campfire.feature.main.home.onboarding.page.userData.UserDataViewModel
import com.pandulapeter.campfire.feature.main.home.onboarding.page.welcome.WelcomeViewModel
import com.pandulapeter.campfire.feature.main.manageDownloads.ManageDownloadsViewModel
import com.pandulapeter.campfire.feature.main.managePlaylists.ManagePlaylistsViewModel
import com.pandulapeter.campfire.feature.main.options.OptionsViewModel
import com.pandulapeter.campfire.feature.main.options.about.AboutViewModel
import com.pandulapeter.campfire.feature.main.options.changelog.ChangelogViewModel
import com.pandulapeter.campfire.feature.main.options.preferences.PreferencesViewModel
import com.pandulapeter.campfire.feature.main.playlist.PlaylistViewModel
import com.pandulapeter.campfire.feature.main.songs.SearchControlsViewModel
import com.pandulapeter.campfire.feature.main.songs.SongsViewModel
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.integration.AppShortcutManager
import com.pandulapeter.campfire.integration.DeepLinkManager
import com.pandulapeter.campfire.integration.FirstTimeUserExperienceManager
import org.koin.android.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val integrationModule = module {
    factory { AppShortcutManager(get(), get(), get()) }
    factory { DeepLinkManager() }
    factory { FirstTimeUserExperienceManager(get()) }
}

val networkingModule = module {
    single { GsonBuilder().create() }
    factory { AnalyticsManager(get(), get(), get()) }
    single { NetworkManager(get()) }
}

val repositoryModule = module {
    single { SongRepository(get(), get(), get()) }
    single { SongDetailRepository(get(), get()) }
    single { ChangelogRepository() }
    single { HistoryRepository(get()) }
    single { PlaylistRepository(get()) }
    single { CollectionRepository(get(), get(), get()) }
}

val persistenceModule = module {
    single { PreferenceDatabase(get()) }
    single { Room.databaseBuilder(get(), Database::class.java, "songDatabase.db").build() }
}

val detailModule = module {
    single { DetailEventBus() }
    single { DetailPageEventBus() }
    factory { SongParser(get()) }
}

val featureModule = module {
    viewModel { AboutViewModel(get()) }
    viewModel { CollectionsViewModel(get(), get(), get(), get()) }
    viewModel { CollectionDetailViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { ManageDownloadsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { PlaylistViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SongsViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SearchControlsViewModel(get()) }
    viewModel { ChangelogViewModel(get()) }
    viewModel { WelcomeViewModel(get()) }
    viewModel { OptionsViewModel(get()) }
    viewModel { SongAppearanceViewModel(get()) }
    viewModel { UserDataViewModel(get(), get()) }
    viewModel { OnboardingViewModel() }
    viewModel { PreferencesViewModel(get(), get(), get()) }
    viewModel { ManagePlaylistsViewModel(get(), get(), get()) }
    viewModel { HomeContainerViewModel(get(), get(), get()) }
    viewModel { ContentLanguageViewModel(get(), get(), get()) }
    viewModel { DetailPageViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { DetailViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
}