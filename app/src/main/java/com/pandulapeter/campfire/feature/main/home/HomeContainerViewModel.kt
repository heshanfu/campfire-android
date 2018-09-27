package com.pandulapeter.campfire.feature.main.home

import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.CollectionRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.CampfireActivity
import com.pandulapeter.campfire.feature.shared.CampfireViewModel

class HomeContainerViewModel(
    preferenceDatabase: PreferenceDatabase,
    collectionRepository: CollectionRepository,
    songRepository: SongRepository
) : CampfireViewModel() {

    init {
        preferenceDatabase.lastScreen = CampfireActivity.SCREEN_HOME

        // Calling any method from the lazily initialized repositories ensures that the data starts loading in the background.
        collectionRepository.isCacheLoaded()
        songRepository.isCacheLoaded()
    }
}