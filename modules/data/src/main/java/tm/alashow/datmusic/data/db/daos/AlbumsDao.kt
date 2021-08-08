/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.data.db.daos

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import tm.alashow.data.db.PaginatedEntryDao
import tm.alashow.datmusic.data.repos.search.DatmusicSearchParams
import tm.alashow.datmusic.domain.entities.Album

@Dao
abstract class AlbumsDao : PaginatedEntryDao<DatmusicSearchParams, Album>() {
    @Transaction
    @Query("SELECT * FROM albums WHERE params = :params and page = :page ORDER BY page ASC, search_index ASC")
    abstract override fun entriesObservable(params: DatmusicSearchParams, page: Int): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM albums ORDER BY page ASC, search_index ASC LIMIT :count OFFSET :offset")
    abstract override fun entriesObservable(count: Int, offset: Int): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM albums ORDER BY page ASC, search_index ASC")
    abstract override fun entriesPagingSource(): PagingSource<Int, Album>

    @Transaction
    @Query("SELECT * FROM albums WHERE params = :params ORDER BY page ASC, search_index ASC")
    abstract override fun entriesPagingSource(params: DatmusicSearchParams): PagingSource<Int, Album>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :id ORDER BY details_fetched")
    abstract override fun entry(id: String): Flow<Album>

    @Transaction
    @Query("SELECT * FROM albums WHERE id in (:ids)")
    abstract override fun entriesById(ids: List<String>): Flow<List<Album>>

    @Transaction
    @Query("SELECT * FROM albums WHERE id = :id")
    abstract override fun entryNullable(id: String): Flow<Album?>

    @Query("DELETE FROM albums WHERE id = :id")
    abstract override suspend fun delete(id: String)

    @Query("DELETE FROM albums WHERE params = :params")
    abstract override suspend fun delete(params: DatmusicSearchParams)

    @Query("DELETE FROM albums WHERE params = :params and page = :page")
    abstract override suspend fun delete(params: DatmusicSearchParams, page: Int)

    @Query("DELETE FROM albums")
    abstract override suspend fun deleteAll()

    @Query("SELECT MAX(page) from albums WHERE params = :params")
    abstract override suspend fun getLastPage(params: DatmusicSearchParams): Int?

    @Query("SELECT COUNT(*) from albums where params = :params")
    abstract override suspend fun count(params: DatmusicSearchParams): Int

    @Query("SELECT COUNT(*) from albums where id = :id")
    abstract override suspend fun has(id: String): Int
}
