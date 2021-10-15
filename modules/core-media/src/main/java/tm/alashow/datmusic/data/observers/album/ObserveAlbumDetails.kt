/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.observers.album

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import tm.alashow.data.SubjectInteractor
import tm.alashow.datmusic.data.DatmusicAlbumParams
import tm.alashow.datmusic.data.db.daos.AlbumsDao
import tm.alashow.datmusic.data.interactors.album.GetAlbumDetails
import tm.alashow.datmusic.domain.entities.Album
import tm.alashow.datmusic.domain.entities.Audio
import tm.alashow.domain.models.Async
import tm.alashow.domain.models.Fail
import tm.alashow.domain.models.Loading
import tm.alashow.domain.models.Success

class ObserveAlbum @Inject constructor(
    private val albumsDao: AlbumsDao,
) : SubjectInteractor<DatmusicAlbumParams, Album>() {
    override fun createObservable(params: DatmusicAlbumParams): Flow<Album> = albumsDao.entry(params.id.toString())
}

class ObserveAlbumDetails @Inject constructor(
    private val getAlbumDetails: GetAlbumDetails,
) : SubjectInteractor<GetAlbumDetails.Params, Async<List<Audio>>>() {

    override fun createObservable(params: GetAlbumDetails.Params) = flow {
        emit(Loading())
        getAlbumDetails(params)
            .catch { error -> emit(Fail<List<Audio>>(error)) }
            .collect { emit(Success(it)) }
    }
}
