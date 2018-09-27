package com.pandulapeter.campfire.feature.main.songs

import android.databinding.ObservableBoolean
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.util.onPropertyChanged

class SearchControlsViewModel(private val preferenceDatabase: PreferenceDatabase) : CampfireViewModel() {

    val isVisible = ObservableBoolean()
    val searchInArtists = ObservableBoolean(preferenceDatabase.shouldSearchInArtists)
    val searchInTitles = ObservableBoolean(preferenceDatabase.shouldSearchInTitles)

    init {
        searchInArtists.onPropertyChanged {
            if (!it) {
                searchInTitles.set(true)
            }
            preferenceDatabase.shouldSearchInArtists = it
        }
        searchInTitles.onPropertyChanged {
            if (!it) {
                searchInArtists.set(true)
            }
            preferenceDatabase.shouldSearchInTitles = it
        }
    }
}