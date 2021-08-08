/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import tm.alashow.datmusic.data.interactors.GetArtistDetails
import tm.alashow.datmusic.data.observers.ObserveArtist
import tm.alashow.datmusic.data.observers.ObserveArtistDetails
import tm.alashow.datmusic.data.repos.artist.DatmusicArtistParams
import tm.alashow.navigation.ARTIST_ID_KEY

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val artist: ObserveArtist,
    private val artistDetails: ObserveArtistDetails
) : ViewModel() {

    private val artistParams = DatmusicArtistParams(handle.get<String>(ARTIST_ID_KEY)!!)

    val state = combine(artist.observe(), artistDetails.observe(), ::ArtistDetailViewState).shareIn(viewModelScope, SharingStarted.Lazily, 1)

    init {
        load()
    }

    private fun load(forceRefresh: Boolean = false) = viewModelScope.launch {
        artist(artistParams)
        artistDetails(GetArtistDetails.Params(artistParams, forceRefresh))
    }

    fun refresh() = load(true)
}
