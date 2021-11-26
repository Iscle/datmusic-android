/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.repos.audio

import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import tm.alashow.base.util.CoroutineDispatchers
import tm.alashow.data.db.RoomRepo
import tm.alashow.datmusic.data.db.daos.AudiosDao
import tm.alashow.datmusic.data.db.daos.DownloadRequestsDao
import tm.alashow.datmusic.domain.entities.Audio
import tm.alashow.datmusic.domain.entities.AudioId
import tm.alashow.datmusic.domain.entities.AudioIds
import tm.alashow.datmusic.domain.entities.Audios

enum class AudioSaveType {
    Download, Playlist;

    fun toAudioParams() = "save_type=${toString()}"
}

class AudiosRepo @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val dao: AudiosDao,
    private val downloadsRequestsDao: DownloadRequestsDao,
) : RoomRepo<AudioId, Audio>(dao, dispatchers) {

    suspend fun audiosById(ids: AudioIds) = dao.audiosById(ids)

    suspend fun saveAudiosById(type: AudioSaveType, audioIds: AudioIds) = saveAudios(type, audiosById(audioIds))

    suspend fun saveAudios(type: AudioSaveType, audios: Audios) = saveAudios(type, *audios.toTypedArray())

    suspend fun saveAudios(type: AudioSaveType, vararg audios: Audio): Int {
        val mapped = audios.map { it.copy(primaryKey = it.id, params = type.toAudioParams()) }
        return insertAll(mapped).size
    }

    private suspend fun findFromAudiosById(ids: AudioIds) = audiosById(ids)
    private suspend fun findAudioDownloadsById(ids: AudioIds) = downloadsRequestsDao.entriesById(ids).firstOrNull().orEmpty()

    suspend fun find(audioId: String): Audio? = find(listOf(audioId)).firstOrNull()

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun find(ids: AudioIds): List<Audio> {
        val audios = findFromAudiosById(ids).map { it.id to it }.toMap()
        val downloads = findAudioDownloadsById(ids).map { it.audio.id to it.audio }.toMap()
        return buildList {
            ids.forEach { id ->
                val audio = audios[id] ?: downloads[id]
                if (audio == null) {
                    Timber.e("Couldn't find audio by id: $id")
                } else add(audio)
            }
        }
    }

    /**
     * Finds missing audio ids from given ids. Tries to recover missing ids from downloads via [findAudioDownloadsById].
     */
    suspend fun findMissingIds(ids: AudioIds): AudioIds {
        val existingIds = findFromAudiosById(ids).map { it.id }.toSet()
        val missingIds = ids.filterNot { existingIds.contains(it) }

        val recoveredAudios = findAudioDownloadsById(missingIds).map { it.audio }
        val recoveredAudioIds = recoveredAudios.map { it.id }.toSet()
        insertAll(recoveredAudios)

        return missingIds.filterNot { recoveredAudioIds.contains(it) }
    }
}
