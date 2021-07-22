/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.albums

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.google.accompanist.placeholder.shimmer
import tm.alashow.datmusic.domain.entities.Album
import tm.alashow.ui.components.CoverImage
import tm.alashow.ui.theme.AppTheme

object AlbumsDefaults {
    val imageSize = 150.dp
    val iconPadding = 36.dp
}

@Composable
fun AlbumColumn(
    album: Album,
    imageSize: Dp = AlbumsDefaults.imageSize,
    iconPadding: Dp = AlbumsDefaults.iconPadding,
    isPlaceholder: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (Album) -> Unit = {},
) {
    val loadingModifier = Modifier.placeholder(
        visible = isPlaceholder,
        highlight = PlaceholderHighlight.shimmer(),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.specs.paddingSmall),
        modifier = modifier
            .clickable { if (!isPlaceholder) onClick(album) }
            .fillMaxWidth()
            .padding(AppTheme.specs.padding)
    ) {
        val image = rememberCoilPainter(album.photo.mediumUrl, fadeIn = true)
        CoverImage(
            image, size = imageSize,
            icon = rememberVectorPainter(Icons.Default.Album),
            iconPadding = iconPadding
        ) { modifier ->
            Image(
                painter = image,
                contentDescription = null,
                modifier = modifier.composed { loadingModifier }
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(AppTheme.specs.paddingTiny),
            modifier = Modifier
                .width(imageSize)
        ) {

            Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = loadingModifier)
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(album.artists.firstOrNull()?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = loadingModifier)
                Text(album.year.toString(), modifier = loadingModifier)
            }
        }
    }
}
