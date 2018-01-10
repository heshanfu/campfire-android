package com.pandulapeter.campfire.inversionOfControl.module

import android.content.Context
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.storage.DataStorageManager
import com.pandulapeter.campfire.integration.AppShortcutManager
import com.pandulapeter.campfire.integration.DeepLinkManager
import com.pandulapeter.campfire.inversionOfControl.app.AppContext
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object IntegrationModule {

    @Provides
    @Singleton
    @JvmStatic
    fun provideAppShortcutManager(
        @AppContext context: Context,
        dataStorageManager: DataStorageManager,
        playlistRepository: PlaylistRepository) = AppShortcutManager(context, playlistRepository, dataStorageManager)

    @Provides
    @Singleton
    @JvmStatic
    fun provideDeepLinkManager() = DeepLinkManager()
}