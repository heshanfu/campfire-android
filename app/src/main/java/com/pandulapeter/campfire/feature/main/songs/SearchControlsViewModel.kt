package com.pandulapeter.campfire.feature.main.songs

import android.databinding.ObservableBoolean
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.util.onPropertyChanged
import org.koin.android.ext.android.inject

class SearchControlsViewModel(val isForCollections: Boolean) : CampfireViewModel() {

    private val preferenceDatabase by inject<PreferenceDatabase>()
    val isVisible = ObservableBoolean()
    val searchInArtists = ObservableBoolean(if (isForCollections) preferenceDatabase.shouldSearchInCollectionDescriptions else preferenceDatabase.shouldSearchInArtists)
    val searchInTitles = ObservableBoolean(if (isForCollections) preferenceDatabase.shouldSearchInCollectionTitles else preferenceDatabase.shouldSearchInTitles)

    init {
        searchInArtists.onPropertyChanged {
            if (!it) {
                searchInTitles.set(true)
            }
            if (isForCollections) {
                preferenceDatabase.shouldSearchInCollectionDescriptions = it
            } else {
                preferenceDatabase.shouldSearchInArtists = it
            }
        }
        searchInTitles.onPropertyChanged {
            if (!it) {
                searchInArtists.set(true)
            }
            if (isForCollections) {
                preferenceDatabase.shouldSearchInCollectionTitles = it
            } else {
                preferenceDatabase.shouldSearchInTitles = it
            }
        }
    }
}