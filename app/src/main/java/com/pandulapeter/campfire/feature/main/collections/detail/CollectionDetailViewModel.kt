package com.pandulapeter.campfire.feature.main.collections.detail

import android.content.Context
import com.pandulapeter.campfire.data.model.remote.Collection
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.CollectionRepository
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.main.collections.CollectionListItemViewModel
import com.pandulapeter.campfire.feature.main.shared.baseSongList.BaseSongListViewModel
import com.pandulapeter.campfire.feature.main.shared.baseSongList.SongListItemViewModel
import com.pandulapeter.campfire.integration.AnalyticsManager

class CollectionDetailViewModel(
    context: Context,
    songRepository: SongRepository,
    songDetailRepository: SongDetailRepository,
    preferenceDatabase: PreferenceDatabase,
    playlistRepository: PlaylistRepository,
    analyticsManager: AnalyticsManager,
    val collectionRepository: CollectionRepository
) : BaseSongListViewModel(
    context,
    songRepository,
    songDetailRepository,
    preferenceDatabase,
    playlistRepository,
    analyticsManager
) {

    override val cardTransitionName get() = "card-${currentCollection?.id}"
    override val imageTransitionName get() = "image-${currentCollection?.id}"
    override val screenName = AnalyticsManager.PARAM_VALUE_SCREEN_COLLECTION_DETAIL
    lateinit var onDataLoaded: () -> Unit
    var currentCollection: Collection? = null
        set(value) {
            field = value
            if (value != null) {
                collection.set(CollectionListItemViewModel.CollectionViewModel(value, newTagText))
            }

        }

    override fun onSongRepositoryDataUpdated(data: List<Song>) {
        super.onSongRepositoryDataUpdated(data)
        if (data.isNotEmpty()) {
            onDataLoaded()
        }
    }

    override fun onActionButtonClicked() = updateData()

    override fun Sequence<Song>.createViewModels() = (collection.get()?.collection?.songs ?: listOf())
        .asSequence()
        .mapNotNull { songId -> find { it.id == songId } }
        .map {
            SongListItemViewModel.SongViewModel(
                newVersionText = newVersionText,
                newTagText = newTagText,
                songDetailRepository = songDetailRepository,
                playlistRepository = playlistRepository,
                song = it
            )
        }
        .toList()

    fun restoreToolbarButtons() {
        if (adapter.items.isNotEmpty()) {
            onDataLoaded()
        }
    }
}