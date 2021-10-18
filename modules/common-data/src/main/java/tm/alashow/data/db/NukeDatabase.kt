/*
 * Copyright (C) 2018, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.data.db

import androidx.room.RoomDatabase
import javax.inject.Inject

/**
 * Tiny class for clearing all tables of database.
 */
class NukeDatabase @Inject constructor(
    private val database: RoomDatabase
) {
    suspend fun nuke() = database.clearAllTables()
}
