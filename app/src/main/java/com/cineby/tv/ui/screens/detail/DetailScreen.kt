package com.cineby.tv.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.cineby.tv.R
import com.cineby.tv.data.model.CastMember
import com.cineby.tv.data.model.EpisodeItem
import com.cineby.tv.ui.components.TvErrorRetry
import com.cineby.tv.ui.components.TvHeroSkeleton
import com.cineby.tv.ui.components.tvCardBorder
import com.cineby.tv.ui.components.tvCardGlow
import com.cineby.tv.ui.components.tvCardScale
import com.cineby.tv.ui.components.tvCardShape
import com.cineby.tv.ui.theme.TvBackground
import com.cineby.tv.ui.theme.TvSurface
import com.cineby.tv.ui.theme.TvGold
import com.cineby.tv.ui.theme.TvMuted
import com.cineby.tv.ui.theme.TvBrand
import com.cineby.tv.ui.theme.TvOnSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    contentType: String,
    slug: String,
    onNavigateToPlayer: (contentType: String, contentId: String, title: String, poster: String?, backdrop: String?, slug: String?, season: Int?, episode: Int?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel()
) {
    LaunchedEffect(contentType, slug) {
        viewModel.loadDetail(contentType, slug)
    }

    val uiState by viewModel.uiState.collectAsState()
    val playButtonFocusRequester = remember { FocusRequester() }

    // Place default focus on "Putar Sekarang" / "Putar Episode 1" as soon as media details load
    LaunchedEffect(uiState.isLoading, uiState.movie, uiState.series) {
        if (!uiState.isLoading && (uiState.movie != null || uiState.series != null)) {
            delay(120)
            try {
                playButtonFocusRequester.requestFocus()
            } catch (e: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        if (uiState.isLoading) {
            TvHeroSkeleton()
        } else if (uiState.error != null) {
            TvErrorRetry(
                message = uiState.error ?: "",
                onRetry = { viewModel.loadDetail(contentType, slug) }
            )
        } else {
            val title = uiState.movie?.title ?: uiState.series?.title ?: ""
            val overview = uiState.movie?.overview ?: uiState.series?.overview ?: ""
            val backdropUrl = uiState.movie?.fullBackdropUrl ?: uiState.series?.fullBackdropUrl ?: ""
            val rating = uiState.movie?.voteAverage ?: uiState.series?.voteAverage ?: ""
            val year = (uiState.movie?.releaseDate ?: uiState.series?.firstAirDate)?.take(4) ?: ""
            val genres = (uiState.movie?.genres ?: uiState.series?.genres ?: emptyList()).joinToString(", ") { it.name }
            val castList = uiState.movie?.cast ?: uiState.series?.cast ?: emptyList()

            TvLazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header & Info
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {
                        AsyncImage(
                            model = backdropUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient fade
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            TvBackground.copy(alpha = 0.6f),
                                            TvBackground
                                        ),
                                        startY = 120f
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 48.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = title,
                                color = TvOnSurface,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (rating.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = TvGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$rating / 10",
                                            color = TvOnSurface,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (year.isNotBlank()) {
                                    Text(text = year, color = TvMuted, fontSize = 12.sp)
                                }

                                if (genres.isNotBlank()) {
                                    Text(text = genres, color = TvMuted, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = overview,
                                color = TvMuted,
                                fontSize = 15.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(600.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Play Button with default focus and clear white outline ring
                            Button(
                                onClick = {
                                    if (contentType == "movie") {
                                        uiState.movie?.let {
                                            onNavigateToPlayer("movie", it.id, it.title, it.fullPosterUrl, it.fullBackdropUrl, it.slug, null, null)
                                        }
                                    } else {
                                        val firstEp = uiState.episodes.firstOrNull()
                                            ?: uiState.series?.defaultSeason?.episodes?.firstOrNull()
                                        if (firstEp != null) {
                                            onNavigateToPlayer(
                                                "episode",
                                                firstEp.id,
                                                "${uiState.series?.title} - ${firstEp.name}",
                                                firstEp.fullStillUrl,
                                                uiState.series?.fullBackdropUrl,
                                                uiState.series?.slug,
                                                uiState.selectedSeason?.seasonNumber ?: 1,
                                                firstEp.episodeNumber
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = TvBrand,
                                    contentColor = TvOnSurface,
                                    focusedContainerColor = TvBrand,
                                    focusedContentColor = TvOnSurface
                                ),
                                scale = ButtonDefaults.scale(focusedScale = 1.08f),
                                border = ButtonDefaults.border(
                                    focusedBorder = Border(
                                        border = BorderStroke(2.dp, TvOnSurface)
                                    )
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp)),
                                modifier = Modifier.focusRequester(playButtonFocusRequester)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = stringResource(R.string.play)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (contentType == "movie") {
                                        stringResource(R.string.play_now)
                                    } else {
                                        stringResource(R.string.play_episode_1)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // If Series: Season selector & Episode list
                if (contentType == "tv_series" && uiState.series != null) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 48.dp, vertical = 14.dp)) {
                            Text(
                                text = stringResource(R.string.select_season_episode),
                                color = TvOnSurface,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Seasons Row
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                pivotOffsets = PivotOffsets(parentFraction = 0.15f, childFraction = 0f)
                            ) {
                                items(uiState.series?.seasons ?: emptyList()) { season ->
                                    val isSelected = season.id == uiState.selectedSeason?.id
                                    Button(
                                        onClick = { viewModel.selectSeason(slug, season) },
                                        colors = ButtonDefaults.colors(
                                            containerColor = if (isSelected) TvBrand else TvSurface,
                                            contentColor = TvOnSurface
                                        ),
                                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp))
                                    ) {
                                        Text(text = season.name)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Episodes List
                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                pivotOffsets = PivotOffsets(parentFraction = 0.15f, childFraction = 0f)
                            ) {
                                items(uiState.episodes, key = { it.id }) { ep ->
                                    TvEpisodeCard(
                                        episode = ep,
                                        onClick = {
                                            onNavigateToPlayer(
                                                "episode",
                                                ep.id,
                                                "${uiState.series?.title} - ${ep.name}",
                                                ep.fullStillUrl,
                                                uiState.series?.fullBackdropUrl,
                                                uiState.series?.slug,
                                                uiState.selectedSeason?.seasonNumber ?: 1,
                                                ep.episodeNumber
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cast list
                if (castList.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(start = 48.dp, top = 16.dp, bottom = 32.dp)) {
                            Text(
                                text = stringResource(R.string.cast),
                                color = TvOnSurface,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            TvLazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                pivotOffsets = PivotOffsets(parentFraction = 0.15f, childFraction = 0f)
                            ) {
                                items(castList.take(12), key = { it.id }) { cast ->
                                    TvCastCard(cast = cast)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEpisodeCard(
    episode: EpisodeItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(220.dp)
            .height(180.dp),
        shape = tvCardShape(),
        colors = CardDefaults.colors(
            containerColor = TvSurface
        ),
        scale = tvCardScale(),
        glow = tvCardGlow(),
        border = tvCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = episode.fullStillUrl,
                    contentDescription = episode.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color(0xBB000000), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.episode_badge, episode.episodeNumber),
                        color = TvOnSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.name}",
                    color = TvOnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = episode.overview ?: "",
                    color = TvMuted,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvCastCard(cast: CastMember) {
    Card(
        onClick = {},
        modifier = Modifier
            .width(105.dp)
            .padding(vertical = 4.dp),
        shape = tvCardShape(),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = TvSurface
        ),
        scale = tvCardScale(),
        glow = tvCardGlow(),
        border = tvCardBorder()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            AsyncImage(
                model = cast.fullProfileUrl,
                contentDescription = cast.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TvSurface)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = cast.name,
                color = TvOnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = cast.character ?: "",
                color = TvMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
