package com.pandulapeter.campfire.feature.main.home.onboarding.page.contentLanguage

import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import com.pandulapeter.campfire.data.model.local.Language
import com.pandulapeter.campfire.data.model.remote.Collection
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.CollectionRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.feature.shared.widget.StateLayout
import com.pandulapeter.campfire.util.onPropertyChanged

class ContentLanguageViewModel(
    private val preferenceDatabase: PreferenceDatabase,
    private val collectionRepository: CollectionRepository,
    private val songRepository: SongRepository
) : CampfireViewModel(), CollectionRepository.Subscriber, SongRepository.Subscriber {

    private var areCollectionsLoading = true
    private var areSongsLoading = true
    val state = ObservableField<StateLayout.State>(StateLayout.State.LOADING)
    val shouldShowError = ObservableBoolean()
    val shouldShowExplicit = ObservableBoolean(preferenceDatabase.shouldShowExplicit)
    lateinit var onLanguagesLoaded: (List<Language>) -> Unit

    init {
        shouldShowExplicit.onPropertyChanged { preferenceDatabase.shouldShowExplicit = it }
        collectionRepository.subscribe(this)
        songRepository.subscribe(this)
    }

    override fun onCleared() {
        collectionRepository.unsubscribe(this)
        songRepository.unsubscribe(this)
    }

    override fun onCollectionsUpdated(data: List<Collection>) = Unit

    override fun onCollectionsLoadingStateChanged(isLoading: Boolean) {
        areCollectionsLoading = isLoading && collectionRepository.languages.isEmpty()
        refreshLoadingState()
        if (!areCollectionsLoading) {
            updateLanguages()
        }
    }

    override fun onCollectionRepositoryUpdateError() = state.set(StateLayout.State.ERROR)

    override fun onSongRepositoryDataUpdated(data: List<Song>) = Unit

    override fun onSongRepositoryLoadingStateChanged(isLoading: Boolean) {
        areSongsLoading = isLoading && songRepository.languages.isEmpty()
        refreshLoadingState()
        if (!areSongsLoading) {
            updateLanguages()
        }
    }

    override fun onSongRepositoryUpdateError() = state.set(StateLayout.State.ERROR)

    fun startLoading() {
        state.set(StateLayout.State.LOADING)
        collectionRepository.updateData()
        songRepository.updateData()
    }

    private fun refreshLoadingState() {
        if ((areCollectionsLoading || areSongsLoading) && state.get() != StateLayout.State.ERROR) {
            state.set(StateLayout.State.LOADING)
        }
    }

    private fun updateLanguages() {
        if (!areCollectionsLoading && !areSongsLoading) {
            onLanguagesLoaded(collectionRepository.languages.union(songRepository.languages).toList())
            state.set(StateLayout.State.NORMAL)
        }
    }
}