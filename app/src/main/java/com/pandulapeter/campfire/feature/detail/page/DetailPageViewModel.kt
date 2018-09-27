package com.pandulapeter.campfire.feature.detail.page

import android.content.Context
import android.databinding.ObservableField
import android.databinding.ObservableFloat
import android.databinding.ObservableInt
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.data.model.local.SongDetailMetadata
import com.pandulapeter.campfire.data.model.remote.Song
import com.pandulapeter.campfire.data.model.remote.SongDetail
import com.pandulapeter.campfire.data.persistence.PreferenceDatabase
import com.pandulapeter.campfire.data.repository.SongDetailRepository
import com.pandulapeter.campfire.feature.detail.DetailPageEventBus
import com.pandulapeter.campfire.feature.detail.page.parsing.SongParser
import com.pandulapeter.campfire.feature.shared.CampfireViewModel
import com.pandulapeter.campfire.feature.shared.widget.StateLayout
import com.pandulapeter.campfire.integration.AnalyticsManager
import com.pandulapeter.campfire.util.dimension
import com.pandulapeter.campfire.util.onPropertyChanged
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

class DetailPageViewModel(
    context: Context,
    private val songDetailRepository: SongDetailRepository,
    private val preferenceDatabase: PreferenceDatabase,
    private val detailPageEventBus: DetailPageEventBus,
    private val analyticsManager: AnalyticsManager,
    private val songParser: SongParser
) : CampfireViewModel(), SongDetailRepository.Subscriber {

    private var initialTextSize = context.dimension(R.dimen.text_normal)
    private var rawText = ""
    val text = ObservableField<CharSequence>("")
    val state = ObservableField<StateLayout.State>(StateLayout.State.LOADING)
    val textSize = ObservableFloat(preferenceDatabase.fontSize * initialTextSize)
    val transposition = ObservableInt()
    var song: Song? = null
        set(value) {
            field = value
            if (value != null) {
                transposition.set(preferenceDatabase.getTransposition(value.id))
            }
        }
    lateinit var onDataLoaded: () -> Unit

    init {
        transposition.onPropertyChanged {
            var modifiedValue = it
            while (modifiedValue > 6) {
                modifiedValue -= 12
            }
            while (modifiedValue < -6) {
                modifiedValue += 12
            }
            if (modifiedValue != it) {
                transposition.set(modifiedValue)
            } else {
                refreshText()
                song?.let { song ->
                    analyticsManager.onTranspositionChanged(song.id, modifiedValue)
                    preferenceDatabase.setTransposition(song.id, modifiedValue)
                    detailPageEventBus.notifyTranspositionChanged(song.id, modifiedValue)
                }
            }
        }
    }

    override fun subscribe() {
        songDetailRepository.subscribe(this)
        if (text.get().isNullOrEmpty()) {
            loadData()
        }
    }

    override fun unsubscribe() = songDetailRepository.unsubscribe(this)

    override fun onSongDetailRepositoryUpdated(downloadedSongs: List<SongDetailMetadata>) = Unit

    override fun onSongDetailRepositoryDownloadSuccess(songDetail: SongDetail) {
        song?.let { song ->
            if (songDetail.id == song.id) {
                rawText = songDetail.text
                refreshText {
                    state.set(StateLayout.State.NORMAL)
                    onDataLoaded()
                    detailPageEventBus.notifyTranspositionChanged(song.id, transposition.get())
                }
            }
        }
    }

    override fun onSongDetailRepositoryDownloadQueueChanged(songIds: List<String>) {
        song?.let { song ->
            if (songIds.contains(song.id) && text.get().isNullOrEmpty()) {
                state.set(StateLayout.State.LOADING)
            }
        }
    }

    override fun onSongDetailRepositoryDownloadError(song: Song) {
        if (song.id == this.song?.id && text.get().isNullOrEmpty()) {
            analyticsManager.onConnectionError(true, song.id)
            state.set(StateLayout.State.ERROR)
        }
    }

    fun loadData() {
        song?.let { song -> songDetailRepository.getSongDetail(song) }
    }

    fun updateTextSize() = textSize.set(preferenceDatabase.fontSize * initialTextSize)

    fun refreshText(onDone: () -> Unit = {}) {
        launch(UI) {
            text.set(
                withContext(CommonPool) {
                    songParser.parseSong(rawText, preferenceDatabase.shouldShowChords, preferenceDatabase.shouldUseGermanNotation, transposition.get())
                }
            )
            onDone()
        }
    }
}