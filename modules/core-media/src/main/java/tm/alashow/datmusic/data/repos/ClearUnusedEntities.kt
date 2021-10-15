/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.repos

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import tm.alashow.datmusic.data.db.daos.AlbumsDao
import tm.alashow.datmusic.data.db.daos.ArtistsDao
import tm.alashow.datmusic.data.db.daos.AudiosDao
import tm.alashow.datmusic.data.db.daos.DownloadRequestsDao
import tm.alashow.datmusic.data.db.daos.PlaylistsWithAudiosDao
import tm.alashow.datmusic.domain.entities.DownloadRequest

class ClearUnusedEntities @Inject constructor(
    private val audiosDao: AudiosDao,
    private val artistsDao: ArtistsDao,
    private val albumsDao: AlbumsDao,
    private val downloadsRequestsDao: DownloadRequestsDao,
    private val playlistWithAudios: PlaylistsWithAudiosDao,
) {
    /**
     * Delete all audios except the ones are in downloads and delete all artists/albums.
     * To be modified to not delete downloaded audios artists/albums in the future.
     */
    suspend operator fun invoke() {
        val downloadRequestAudios = downloadsRequestsDao.entriesObservableByType(DownloadRequest.Type.Audio).first()
        val downloadedAudioIds = downloadRequestAudios.map { it.entityId }
        val audioIdsInPlaylists = playlistWithAudios.distinctAudios().first()

        val audioIds = downloadedAudioIds + audioIdsInPlaylists

        // val downloadedAudios = audiosDao.entriesById(audioIds).first()
        // val downloadedArtistNames = downloadedAudios.map { it.artists() }.flatten().toSet()
        // val downloadedAlbumTitles = downloadedAudios.map { it.album }.filterNotNull().toSet()

        val deletedAudios = audiosDao.deleteExcept(audioIds)
        val deletedArtists = artistsDao.deleteAll()
        val deletedAlbums = albumsDao.deleteAll()
        Timber.d("deletedAudios: $deletedAudios, deletedArtists: $deletedArtists, deletedAlbums: $deletedAlbums")
    }
}
