package com.pandulapeter.campfire.feature.main.shared.baseSongList

import android.content.Context
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import android.databinding.ObservableInt
import android.support.annotation.CallSuper
import android.support.v7.widget.RecyclerView
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.Playlist
import com.pandulapeter.campfire.data.model.local.SongDetailMetadata
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.model.remote.SongDetail
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.PlaylistRepository
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.data.repository.SongRepository
import com.pandulapeter.campfire.feature.main.collections.CollectionListItemViewModel
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.feature.shared.widget.StateLayout
import com.pandulapeter.campfire.integration.AnalyticsManager
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import kotlin.coroutines.experimental.CoroutineContext

abstract class BaseSongListViewModel(
    context: Context,
    protected val songRepository: SongRepository,
    protected val songDetailRepository: SongDetailRepository,
    protected val preferenceDatabase: PreferenceDatabase,
    protected val playlistRepository: PlaylistRepository,
    protected val analyticsManager: AnalyticsManager
) : CampfireViewModel(), SongRepository.Subscriber, SongDetailRepository.Subscriber,
    PlaylistRepository.Subscriber {

    abstract val screenName: String
    private var coroutine: CoroutineContext? = null
    protected var songs = sequenceOf<Song>()
    val collection = ObservableField<CollectionListItemViewModel.CollectionViewModel?>()
    val adapter = SongListAdapter()
    val shouldShowUpdateErrorSnackbar = ObservableBoolean()
    val downloadSongError = ObservableField<Song?>()
    val isLoading = ObservableBoolean()
    val state = ObservableField<StateLayout.State>(StateLayout.State.LOADING)
    val placeholderText = ObservableInt(R.string.songs_initializing_error)
    val buttonText = ObservableInt(R.string.try_again)
    val buttonIcon = ObservableInt()
    var isDetailScreenOpen = false
    protected val newVersionText: String = context.getString(R.string.new_version_available)
    protected val newTagText: String = context.getString(R.string.new_tag)
    open val cardTransitionName = ""
    open val imageTransitionName = ""
    lateinit var openSongs: () -> Unit

    init {
        songRepository.subscribe(this)
        songDetailRepository.subscribe(this)
        playlistRepository.subscribe(this)
        isDetailScreenOpen = false
    }

    @CallSuper
    override fun onCleared() {
        songRepository.unsubscribe(this)
        songDetailRepository.unsubscribe(this)
        playlistRepository.unsubscribe(this)
    }

    @CallSuper
    override fun onSongRepositoryDataUpdated(data: List<Song>) {
        songs = data.asSequence()
        updateAdapterItems(true)
    }

    override fun onSongRepositoryLoadingStateChanged(isLoading: Boolean) {
        this.isLoading.set(isLoading)
        if (songs.toList().isEmpty() && isLoading) {
            state.set(StateLayout.State.LOADING)
        }
    }

    override fun onSongRepositoryUpdateError() {
        if (songs.toList().isEmpty()) {
            analyticsManager.onConnectionError(true, screenName)
            state.set(StateLayout.State.ERROR)
        } else {
            analyticsManager.onConnectionError(false, screenName)
            shouldShowUpdateErrorSnackbar.set(true)
        }
    }

    override fun onSongDetailRepositoryUpdated(downloadedSongs: List<SongDetailMetadata>) {
        if (songs.toList().isNotEmpty()) {
            updateAdapterItems()
        }
    }

    override fun onSongDetailRepositoryDownloadSuccess(songDetail: SongDetail) {
        adapter.items.indexOfLast { it is SongListItemViewModel.SongViewModel && it.song.id == songDetail.id }.let { index ->
            if (index != RecyclerView.NO_POSITION && (adapter.items[index] as? SongListItemViewModel.SongViewModel)?.song?.version == songDetail.version) {
                adapter.notifyItemChanged(
                    index,
                    SongListAdapter.Payload.DownloadStateChanged(SongListItemViewModel.SongViewModel.DownloadState.Downloaded.UpToDate)
                )
            }
        }
    }

    override fun onSongDetailRepositoryDownloadQueueChanged(songIds: List<String>) {
        songIds.forEach { songId ->
            adapter.items.indexOfLast { it is SongListItemViewModel.SongViewModel && it.song.id == songId }.let { index ->
                if (index != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(
                        index,
                        SongListAdapter.Payload.DownloadStateChanged(SongListItemViewModel.SongViewModel.DownloadState.Downloading)
                    )
                }
            }
        }
    }

    override fun onSongDetailRepositoryDownloadError(song: Song) {
        analyticsManager.onConnectionError(!songDetailRepository.isSongDownloaded(song.id), song.id)
        downloadSongError.set(song)
        adapter.items.indexOfLast { it is SongListItemViewModel.SongViewModel && it.song.id == song.id }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    SongListAdapter.Payload.DownloadStateChanged(
                        when {
                            songDetailRepository.isSongDownloaded(song.id) -> SongListItemViewModel.SongViewModel.DownloadState.Downloaded.Deprecated
                            song.isNew -> SongListItemViewModel.SongViewModel.DownloadState.NotDownloaded.New
                            else -> SongListItemViewModel.SongViewModel.DownloadState.NotDownloaded.Old
                        }
                    )
                )
            }
        }
    }

    override fun onPlaylistsUpdated(playlists: List<Playlist>) {
        if (songs.toList().isNotEmpty()) {
            updateAdapterItems()
        }
    }

    override fun onSongAddedToPlaylistForTheFirstTime(songId: String) {
        adapter.items.indexOfLast { it is SongListItemViewModel.SongViewModel && it.song.id == songId }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    SongListAdapter.Payload.IsSongInAPlaylistChanged(true)
                )
            }
        }
    }

    override fun onSongRemovedFromAllPlaylists(songId: String) {
        adapter.items.indexOfLast { it is SongListItemViewModel.SongViewModel && it.song.id == songId }.let { index ->
            if (index != RecyclerView.NO_POSITION) {
                adapter.notifyItemChanged(
                    index,
                    SongListAdapter.Payload.IsSongInAPlaylistChanged(false)
                )
            }
        }
    }

    override fun onPlaylistOrderChanged(playlists: List<Playlist>) = Unit

    abstract fun onActionButtonClicked()

    fun updateData() = songRepository.updateData()

    fun downloadSong(song: Song) = songDetailRepository.getSongDetail(song, true)

    fun areThereMoreThanOnePlaylists() = playlistRepository.cache.size > 1

    fun toggleFavoritesState(songId: String) {
        if (playlistRepository.isSongInPlaylist(Playlist.FAVORITES_ID, songId)) {
            analyticsManager.onSongPlaylistStateChanged(
                songId,
                playlistRepository.getPlaylistCountForSong(songId) - 1,
                screenName,
                playlistRepository.cache.size > 1
            )
            playlistRepository.removeSongFromPlaylist(Playlist.FAVORITES_ID, songId)
        } else {
            analyticsManager.onSongPlaylistStateChanged(
                songId,
                playlistRepository.getPlaylistCountForSong(songId) + 1,
                screenName,
                playlistRepository.cache.size > 1
            )
            playlistRepository.addSongToPlaylist(Playlist.FAVORITES_ID, songId)
        }
    }

    protected open fun canUpdateUI() = true

    protected abstract fun Sequence<Song>.createViewModels(): List<SongListItemViewModel>

    @CallSuper
    protected open fun onListUpdated(items: List<SongListItemViewModel>) {
        state.set(if (items.isEmpty()) StateLayout.State.ERROR else StateLayout.State.NORMAL)
    }


    protected fun updateAdapterItems(shouldScrollToTop: Boolean = false) {
        if (canUpdateUI() && playlistRepository.isCacheLoaded() && songRepository.isCacheLoaded() && songDetailRepository.isCacheLoaded()) {
            coroutine?.cancel()
            coroutine = launch(UI) {
                withContext(CommonPool) { songs.createViewModels() }.let {
                    adapter.shouldScrollToTop = shouldScrollToTop
                    adapter.items = it
                    onListUpdated(it)
                }
                coroutine = null
            }
        }
    }
}