/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.playback

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import tm.alashow.base.imageloading.ImageLoading
import tm.alashow.base.util.extensions.orNA
import tm.alashow.common.compose.LocalPlaybackConnection
import tm.alashow.common.compose.rememberFlowWithLifecycle
import tm.alashow.datmusic.domain.entities.Audio
import tm.alashow.datmusic.playback.NONE_PLAYBACK_STATE
import tm.alashow.datmusic.playback.NONE_PLAYING
import tm.alashow.datmusic.playback.PlaybackConnection
import tm.alashow.datmusic.playback.artwork
import tm.alashow.datmusic.playback.artworkUri
import tm.alashow.datmusic.playback.isActive
import tm.alashow.datmusic.playback.isBuffering
import tm.alashow.datmusic.playback.isError
import tm.alashow.datmusic.playback.isPlayEnabled
import tm.alashow.datmusic.playback.isPlaying
import tm.alashow.datmusic.playback.models.PlaybackProgressState
import tm.alashow.datmusic.playback.playPause
import tm.alashow.navigation.LocalNavigator
import tm.alashow.navigation.Navigator
import tm.alashow.navigation.screens.LeafScreen
import tm.alashow.ui.ADAPTIVE_COLOR_ANIMATION
import tm.alashow.ui.Dismissable
import tm.alashow.ui.adaptiveColor
import tm.alashow.ui.components.CoverImage
import tm.alashow.ui.components.IconButton
import tm.alashow.ui.theme.AppTheme

object PlaybackMiniControlsDefaults {
    val height = 56.dp
    val playPauseSize = 36.dp
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlaybackMiniControls(
    modifier: Modifier = Modifier,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current
) {
    val playbackState by rememberFlowWithLifecycle(playbackConnection.playbackState).collectAsState(NONE_PLAYBACK_STATE)
    val nowPlaying by rememberFlowWithLifecycle(playbackConnection.nowPlaying).collectAsState(NONE_PLAYING)

    val visible = (playbackState to nowPlaying).isActive
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it / 2 }),
        exit = slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        PlaybackMiniControls(
            playbackState = playbackState,
            nowPlaying = nowPlaying,
            onPlayPause = { playbackConnection.mediaController?.playPause() },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlaybackMiniControls(
    playbackState: PlaybackStateCompat,
    nowPlaying: MediaMetadataCompat,
    modifier: Modifier = Modifier,
    onPlayPause: () -> Unit,
    height: Dp = PlaybackMiniControlsDefaults.height,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
    navigator: Navigator = LocalNavigator.current,
) {
    val expand = {
        navigator.navigate(LeafScreen.PlaybackSheet().createRoute())
    }

    val adaptiveColor = adaptiveColor(nowPlaying.artwork, initial = MaterialTheme.colors.background)
    val backgroundColor by animateColorAsState(adaptiveColor.color, ADAPTIVE_COLOR_ANIMATION)
    val contentColor by animateColorAsState(adaptiveColor.contentColor, ADAPTIVE_COLOR_ANIMATION)

    Dismissable(onDismiss = { playbackConnection.transportControls?.stop() }) {
        Surface(
            color = Color.Transparent,
            shape = MaterialTheme.shapes.small,
            modifier = modifier
                .padding(horizontal = AppTheme.specs.paddingSmall)
                .clickable { expand() }
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(height)
                        .fillMaxWidth()
                        .background(backgroundColor)
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        PlaybackNowPlaying(nowPlaying = nowPlaying, maxHeight = height)
                        PlaybackPlayPause(playbackState = playbackState, onPlayPause = onPlayPause)
                    }
                }
                PlaybackProgress(
                    playbackState = playbackState,
                    color = MaterialTheme.colors.onBackground
                )
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    playbackState: PlaybackStateCompat,
    color: Color,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
) {
    val progressState by rememberFlowWithLifecycle(playbackConnection.playbackProgress).collectAsState(PlaybackProgressState())
    val sizeModifier = Modifier
        .height(2.dp)
        .fillMaxWidth()
    when {
        playbackState.isBuffering -> {
            LinearProgressIndicator(
                color = color,
                modifier = sizeModifier
            )
        }
        else -> {
            val progress by animatePlaybackProgress(progressState.progress)
            LinearProgressIndicator(
                progress = progress,
                color = color,
                backgroundColor = color.copy(ProgressIndicatorDefaults.IndicatorBackgroundOpacity),
                modifier = sizeModifier
            )
        }
    }
}

@Composable
private fun RowScope.PlaybackNowPlaying(
    nowPlaying: MediaMetadataCompat,
    maxHeight: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppTheme.specs.paddingTiny),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(7f),
    ) {
        val artwork = rememberImagePainter(nowPlaying.artwork ?: nowPlaying.artworkUri, builder = ImageLoading.defaultConfig)
        CoverImage(
            painter = artwork,
            size = maxHeight - 16.dp,
            modifier = Modifier.padding(AppTheme.specs.paddingSmall)
        )

        PlaybackPager(nowPlaying) { audio, _, pagerMod ->
            PlaybackNowPlaying(audio, modifier = pagerMod)
        }
    }
}

@Composable
private fun PlaybackNowPlaying(audio: Audio, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .padding(vertical = AppTheme.specs.paddingSmall)
            .fillMaxWidth()
            .then(modifier)
    ) {
        Text(
            audio.title.orNA(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold)
        )
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                audio.artist.orNA(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
private fun RowScope.PlaybackPlayPause(
    playbackState: PlaybackStateCompat,
    size: Dp = PlaybackMiniControlsDefaults.playPauseSize,
    onPlayPause: () -> Unit
) {
    IconButton(
        onClick = onPlayPause,
        rippleColor = LocalContentColor.current,
        modifier = Modifier.weight(1f)
    ) {
        Icon(
            painter = rememberVectorPainter(
                when {
                    playbackState.isError -> Icons.Filled.ErrorOutline
                    playbackState.isPlaying -> Icons.Filled.Pause
                    playbackState.isPlayEnabled -> Icons.Filled.PlayArrow
                    else -> Icons.Filled.HourglassBottom
                }
            ),
            modifier = Modifier.size(size),
            contentDescription = null
        )
    }
}
