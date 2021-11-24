/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.playback

import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.RepeatOneOn
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.ShuffleOn
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.pager.rememberPagerState
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tm.alashow.base.ui.ColorPalettePreference
import tm.alashow.base.ui.ThemeState
import tm.alashow.base.util.extensions.Callback
import tm.alashow.base.util.extensions.orNA
import tm.alashow.base.util.extensions.toFloat
import tm.alashow.base.util.millisToDuration
import tm.alashow.common.compose.LocalPlaybackConnection
import tm.alashow.common.compose.LocalScaffoldState
import tm.alashow.common.compose.rememberFlowWithLifecycle
import tm.alashow.datmusic.domain.CoverImageSize
import tm.alashow.datmusic.domain.entities.Audio
import tm.alashow.datmusic.downloader.audioHeader
import tm.alashow.datmusic.playback.*
import tm.alashow.datmusic.playback.models.PlaybackModeState
import tm.alashow.datmusic.playback.models.PlaybackProgressState
import tm.alashow.datmusic.playback.models.PlaybackQueue
import tm.alashow.datmusic.playback.models.QueueTitle.Companion.asQueueTitle
import tm.alashow.datmusic.ui.audios.AudioActionHandler
import tm.alashow.datmusic.ui.audios.AudioDropdownMenu
import tm.alashow.datmusic.ui.audios.AudioItemAction
import tm.alashow.datmusic.ui.audios.AudioRow
import tm.alashow.datmusic.ui.audios.LocalAudioActionHandler
import tm.alashow.datmusic.ui.audios.audioActionHandler
import tm.alashow.datmusic.ui.audios.currentPlayingMenuActionLabels
import tm.alashow.datmusic.ui.library.playlist.addTo.AddToPlaylistMenu
import tm.alashow.navigation.LocalNavigator
import tm.alashow.navigation.Navigator
import tm.alashow.ui.ADAPTIVE_COLOR_ANIMATION
import tm.alashow.ui.Delayed
import tm.alashow.ui.DismissableSnackbarHost
import tm.alashow.ui.adaptiveColor
import tm.alashow.ui.coloredRippleClickable
import tm.alashow.ui.components.CoverImage
import tm.alashow.ui.components.IconButton
import tm.alashow.ui.components.MoreVerticalIcon
import tm.alashow.ui.material.Slider
import tm.alashow.ui.material.SliderDefaults
import tm.alashow.ui.simpleClickable
import tm.alashow.ui.theme.AppTheme
import tm.alashow.ui.theme.LocalThemeState
import tm.alashow.ui.theme.disabledAlpha
import tm.alashow.ui.theme.plainBackgroundColor
import tm.alashow.ui.theme.plainSurfaceColor

private val RemoveFromPlaylist = R.string.playback_queue_removeFromQueue
private val AddQueueToPlaylist = R.string.playback_queue_addQueueToPlaylist
private val SaveQueueAsPlaylist = R.string.playback_queue_saveAsPlaylist

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PlaybackSheet(
    // override local theme color palette because we want simple colors for menus n' stuff
    sheetTheme: ThemeState = LocalThemeState.current.copy(colorPalettePreference = ColorPalettePreference.Black),
    navigator: Navigator = LocalNavigator.current,
) {
    val listState = rememberLazyListState()
    val coroutine = rememberCoroutineScope()

    val scrollToTop: Callback = {
        coroutine.launch {
            listState.animateScrollToItem(0)
        }
    }

    val audioActionHandler = audioActionHandler()
    CompositionLocalProvider(LocalAudioActionHandler provides audioActionHandler) {
        AppTheme(theme = sheetTheme, changeSystemBar = false) {
            PlaybackSheetContent(
                onClose = { navigator.goBack() },
                scrollToTop = scrollToTop,
                listState = listState
            )
        }
    }
}

@Composable
internal fun PlaybackSheetContent(
    onClose: Callback,
    scrollToTop: Callback,
    listState: LazyListState,
    scaffoldState: ScaffoldState = rememberScaffoldState(snackbarHostState = LocalScaffoldState.current.snackbarHostState),
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
    viewModel: PlaybackSheetViewModel = hiltViewModel(),
) {
    val playbackState by rememberFlowWithLifecycle(playbackConnection.playbackState).collectAsState(NONE_PLAYBACK_STATE)
    val playbackQueue by rememberFlowWithLifecycle(playbackConnection.playbackQueue).collectAsState(PlaybackQueue())
    val nowPlaying by rememberFlowWithLifecycle(playbackConnection.nowPlaying).collectAsState(NONE_PLAYING)

    val adaptiveColor = adaptiveColor(nowPlaying.artwork, initial = MaterialTheme.colors.onBackground)
    val contentColor by animateColorAsState(adaptiveColor.color, ADAPTIVE_COLOR_ANIMATION)

    val pagerState = rememberPagerState(playbackQueue.currentIndex)

    LaunchedEffect(playbackConnection) {
        playbackConnection.playbackState.collect {
            if (it.isIdle) onClose()
        }
    }

    Scaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier.background(adaptiveColor.gradient),
        scaffoldState = scaffoldState,
        snackbarHost = {
            DismissableSnackbarHost(it, modifier = Modifier.navigationBarsPadding())
        },
    ) {
        LazyColumn(
            state = listState,
            contentPadding = rememberInsetsPaddingValues(
                insets = LocalWindowInsets.current.systemBars,
                applyTop = true,
                applyBottom = true,
            ),
        ) {
            item {
                PlaybackSheetTopBar(
                    playbackQueue = playbackQueue,
                    onClose = onClose,
                    onTitleClick = viewModel::navigateToQueueSource,
                    onSaveQueueAsPlaylist = viewModel::saveQueueAsPlaylist
                )
                Spacer(Modifier.height(AppTheme.specs.paddingTiny))
            }

            item {
                PlaybackPager(
                    nowPlaying = nowPlaying,
                    modifier = Modifier.fillParentMaxHeight(0.45f),
                    pagerState = pagerState,
                ) { audio, _, pagerMod ->
                    val currentArtwork = audio.coverUri(CoverImageSize.LARGE)
                    PlaybackArtwork(currentArtwork, contentColor, nowPlaying, pagerMod)
                }
            }

            item {
                PlaybackNowPlayingWithControls(
                    nowPlaying = nowPlaying,
                    playbackState = playbackState,
                    contentColor = contentColor,
                    onTitleClick = viewModel::onTitleClick,
                    onArtistClick = viewModel::onArtistClick,
                )
            }

            if (playbackQueue.isValid)
                item {
                    PlaybackAudioInfo(audio = playbackQueue.currentAudio)
                }

            playbackQueue(
                playbackQueue = playbackQueue,
                scrollToTop = scrollToTop,
                playbackConnection = playbackConnection,
            )
        }
    }
}

@Composable
private fun PlaybackSheetTopBar(
    playbackQueue: PlaybackQueue,
    onClose: Callback,
    onTitleClick: Callback,
    onSaveQueueAsPlaylist: Callback,
) {
    TopAppBar(
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        title = { PlaybackSheetTopBarTitle(playbackQueue, onTitleClick) },
        actions = { PlaybackSheetTopBarActions(playbackQueue, onSaveQueueAsPlaylist) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    rememberVectorPainter(Icons.Default.KeyboardArrowDown),
                    modifier = Modifier.size(AppTheme.specs.iconSize),
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun PlaybackSheetTopBarTitle(
    playbackQueue: PlaybackQueue,
    onTitleClick: Callback,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .offset(x = -8.dp) // idk why this is needed for centering
            .simpleClickable(onClick = onTitleClick)
    ) {
        val context = LocalContext.current
        val queueTitle = playbackQueue.title.asQueueTitle()
        Text(
            text = queueTitle.localizeType(context.resources).uppercase(),
            style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Light),
            maxLines = 1,
        )
        Text(
            text = queueTitle.localizeValue(),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}

@Composable
private fun PlaybackSheetTopBarActions(
    playbackQueue: PlaybackQueue,
    onSaveQueueAsPlaylist: Callback,
    actionHandler: AudioActionHandler = LocalAudioActionHandler.current,
) {
    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        if (playbackQueue.isValid) {
            val (addToPlaylistVisible, setAddToPlaylistVisible) = remember { mutableStateOf(false) }
            val (addQueueToPlaylistVisible, setAddQueueToPlaylistVisible) = remember { mutableStateOf(false) }

            AddToPlaylistMenu(playbackQueue.currentAudio, addToPlaylistVisible, setAddToPlaylistVisible)
            AddToPlaylistMenu(playbackQueue.audios, addQueueToPlaylistVisible, setAddQueueToPlaylistVisible)

            AudioDropdownMenu(
                expanded = expanded,
                onExpandedChange = setExpanded,
                actionLabels = currentPlayingMenuActionLabels,
                extraActionLabels = listOf(AddQueueToPlaylist, SaveQueueAsPlaylist)
            ) { actionLabel ->
                val audio = playbackQueue.currentAudio
                when (val action = AudioItemAction.from(actionLabel, audio)) {
                    is AudioItemAction.AddToPlaylist -> setAddToPlaylistVisible(true)
                    else -> {
                        action.handleExtraActions(actionHandler) {
                            when (it.actionLabelRes) {
                                AddQueueToPlaylist -> setAddQueueToPlaylistVisible(true)
                                SaveQueueAsPlaylist -> onSaveQueueAsPlaylist()
                            }
                        }
                    }
                }
            }
        } else MoreVerticalIcon()
    }
}

@Composable
private fun PlaybackArtwork(
    artwork: Uri,
    contentColor: Color,
    nowPlaying: MediaMetadataCompat,
    modifier: Modifier = Modifier,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
) {
    CoverImage(
        data = artwork,
        shape = RectangleShape,
        backgroundColor = MaterialTheme.colors.plainSurfaceColor(),
        contentColor = contentColor,
        bitmapPlaceholder = nowPlaying.artwork,
        modifier = Modifier
            .padding(horizontal = AppTheme.specs.paddingLarge)
            .then(modifier),
        imageModifier = Modifier
            .coloredRippleClickable(
                onClick = {
                    playbackConnection.mediaController?.playPause()
                },
                color = contentColor,
                rippleRadius = Dp.Unspecified,
            ),
    )
}

@Composable
private fun PlaybackNowPlayingWithControls(
    nowPlaying: MediaMetadataCompat,
    playbackState: PlaybackStateCompat,
    contentColor: Color,
    onTitleClick: Callback,
    onArtistClick: Callback,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(AppTheme.specs.paddingLarge)
    ) {
        PlaybackNowPlaying(
            nowPlaying = nowPlaying,
            onTitleClick = onTitleClick,
            onArtistClick = onArtistClick
        )

        PlaybackProgress(
            playbackState = playbackState,
            contentColor = contentColor
        )

        PlaybackControls(
            playbackState = playbackState,
            contentColor = contentColor,
        )
    }
}

@Composable
private fun PlaybackNowPlaying(
    nowPlaying: MediaMetadataCompat,
    onTitleClick: Callback,
    onArtistClick: Callback,
) {
    val title = nowPlaying.title
    Text(
        title.orNA(),
        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        modifier = Modifier.simpleClickable(onClick = onTitleClick)
    )
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            nowPlaying.artist.orNA(),
            style = MaterialTheme.typography.subtitle1,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.simpleClickable(onClick = onArtistClick)
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PlaybackProgress(
    playbackState: PlaybackStateCompat,
    contentColor: Color,
    thumbRadius: Dp = 4.dp,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current
) {
    val progressState by rememberFlowWithLifecycle(playbackConnection.playbackProgress).collectAsState(PlaybackProgressState())

    val (draggingProgress, setDraggingProgress) = remember { mutableStateOf<Float?>(null) }

    Box {
        PlaybackProgressSlider(playbackState, progressState, draggingProgress, setDraggingProgress, thumbRadius, contentColor)
        PlaybackProgressDuration(progressState, draggingProgress, thumbRadius)
    }
}

@Composable
private fun PlaybackProgressSlider(
    playbackState: PlaybackStateCompat,
    progressState: PlaybackProgressState,
    draggingProgress: Float?,
    setDraggingProgress: (Float?) -> Unit,
    thumbRadius: Dp,
    contentColor: Color,
    bufferedProgressColor: Color = contentColor.copy(alpha = 0.25f),
    height: Dp = 44.dp,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current
) {
    val updatedProgressState by rememberUpdatedState(progressState)
    val updatedDraggingProgress by rememberUpdatedState(draggingProgress)

    val sliderColors = SliderDefaults.colors(
        thumbColor = contentColor,
        activeTrackColor = contentColor,
        inactiveTrackColor = contentColor.copy(alpha = ContentAlpha.disabled)
    )
    val linearProgressMod = Modifier
        .fillMaxWidth(fraction = .99f) // reduce linearProgressIndicators width to match Slider's
        .clip(CircleShape) // because Slider is rounded

    val bufferedProgress by animatePlaybackProgress(progressState.bufferedProgress)
    val isBuffering = playbackState.isBuffering
    val sliderProgress = progressState.progress

    Box(
        modifier = Modifier.height(height),
        contentAlignment = Alignment.Center
    ) {
        if (!isBuffering)
            LinearProgressIndicator(
                progress = bufferedProgress,
                color = bufferedProgressColor,
                backgroundColor = Color.Transparent,
                modifier = linearProgressMod
            )

        Slider(
            value = draggingProgress ?: sliderProgress,
            onValueChange = {
                if (!isBuffering) setDraggingProgress(it)
            },
            thumbRadius = thumbRadius,
            colors = sliderColors,
            modifier = Modifier.alpha(isBuffering.not().toFloat()),
            onValueChangeFinished = {
                playbackConnection.transportControls?.seekTo(
                    (updatedProgressState.total.toFloat() * (updatedDraggingProgress ?: 0f)).roundToLong()
                )
                setDraggingProgress(null)
            }
        )

        if (isBuffering) {
            LinearProgressIndicator(
                progress = 0f,
                color = contentColor,
                modifier = linearProgressMod
            )
            Delayed(
                modifier = Modifier
                    .align(Alignment.Center)
                    .then(linearProgressMod)
            ) {
                LinearProgressIndicator(
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.PlaybackProgressDuration(
    progressState: PlaybackProgressState,
    draggingProgress: Float?,
    thumbRadius: Dp
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = thumbRadius)
            .align(Alignment.BottomCenter)
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            val currentDuration = when (draggingProgress != null) {
                true -> (progressState.total.toFloat() * (draggingProgress)).toLong().millisToDuration()
                else -> progressState.currentDuration
            }
            Text(currentDuration, style = MaterialTheme.typography.caption)
            Text(progressState.totalDuration, style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
private fun PlaybackControls(
    playbackState: PlaybackStateCompat,
    contentColor: Color,
    modifier: Modifier = Modifier,
    smallRippleRadius: Dp = 30.dp,
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current
) {
    val playbackMode by rememberFlowWithLifecycle(playbackConnection.playbackMode).collectAsState(PlaybackModeState())
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { playbackConnection.mediaController?.toggleShuffleMode() },
            modifier = Modifier.size(20.dp),
            rippleRadius = smallRippleRadius,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    when (playbackMode.shuffleMode) {
                        PlaybackStateCompat.SHUFFLE_MODE_NONE -> Icons.Default.Shuffle
                        PlaybackStateCompat.SHUFFLE_MODE_ALL -> Icons.Default.ShuffleOn
                        else -> Icons.Default.Shuffle
                    }
                ),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTheme.specs.paddingLarge))

        IconButton(
            onClick = { playbackConnection.transportControls?.skipToPrevious() },
            modifier = Modifier.size(40.dp),
            rippleRadius = smallRippleRadius,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.SkipPrevious),
                tint = contentColor.disabledAlpha(playbackState.hasPrevious),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTheme.specs.padding))

        IconButton(
            onClick = { playbackConnection.mediaController?.playPause() },
            modifier = Modifier.size(80.dp),
            rippleRadius = 35.dp,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    when {
                        playbackState.isError -> Icons.Filled.ErrorOutline
                        playbackState.isPlaying -> Icons.Filled.PauseCircleFilled
                        playbackState.isPlayEnabled -> Icons.Filled.PlayCircleFilled
                        else -> Icons.Filled.PlayCircleFilled
                    }
                ),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTheme.specs.padding))

        IconButton(
            onClick = { playbackConnection.transportControls?.skipToNext() },
            modifier = Modifier.size(40.dp),
            rippleRadius = smallRippleRadius,
        ) {
            Icon(
                painter = rememberVectorPainter(Icons.Default.SkipNext),
                tint = contentColor.disabledAlpha(playbackState.hasNext),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        }

        Spacer(Modifier.width(AppTheme.specs.paddingLarge))

        IconButton(
            onClick = { playbackConnection.mediaController?.toggleRepeatMode() },
            modifier = Modifier.size(20.dp),
            rippleRadius = smallRippleRadius,
        ) {
            Icon(
                painter = rememberVectorPainter(
                    when (playbackMode.repeatMode) {
                        PlaybackStateCompat.REPEAT_MODE_ONE -> Icons.Default.RepeatOneOn
                        PlaybackStateCompat.REPEAT_MODE_ALL -> Icons.Default.RepeatOn
                        else -> Icons.Default.Repeat
                    }
                ),
                tint = contentColor,
                modifier = Modifier.fillMaxSize(),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun PlaybackAudioInfo(audio: Audio) {
    val context = LocalContext.current
    val dlItem = audio.audioDownloadItem
    if (dlItem != null) {
        val audiHeader = dlItem.audioHeader(context)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTheme.specs.padding)
        ) {
            Surface(
                color = MaterialTheme.colors.plainBackgroundColor().copy(alpha = 0.1f),
                shape = CircleShape,
            ) {
                Text(
                    audiHeader.info(),
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
private fun LazyListScope.playbackQueue(
    playbackQueue: PlaybackQueue,
    scrollToTop: Callback,
    playbackConnection: PlaybackConnection,
) {
    val lastIndex = playbackQueue.audios.size
    val firstIndex = (playbackQueue.currentIndex + 1).coerceAtMost(lastIndex)
    val queue = playbackQueue.audios.subList(firstIndex, lastIndex)
    itemsIndexed(queue, key = { _, a -> a.primaryKey }) { index, audio ->
        val realPosition = firstIndex + index
        AudioRow(
            audio = audio,
            observeNowPlayingAudio = false,
            imageSize = 40.dp,
            onPlayAudio = {
                playbackConnection.transportControls?.skipToQueueItem(realPosition.toLong())
                scrollToTop()
            },
            extraActionLabels = listOf(RemoveFromPlaylist),
            onExtraAction = { playbackConnection.removeByPosition(realPosition) },
            modifier = Modifier.animateItemPlacement()
        )
    }
}
