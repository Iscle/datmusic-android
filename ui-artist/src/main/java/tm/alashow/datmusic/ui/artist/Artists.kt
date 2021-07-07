/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.artist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tm.alashow.datmusic.domain.entities.ArtistId
import tm.alashow.ui.theme.randomBackground

@Composable
fun ArtistDetail(artistId: ArtistId) {
    Text(
        artistId,
        Modifier
            .padding(100.dp)
            .randomBackground()
    )
}
