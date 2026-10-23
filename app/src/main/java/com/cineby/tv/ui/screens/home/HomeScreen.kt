package com.cineby.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cineby.tv.R
import com.cineby.tv.data.model.MovieItem
import com.cineby.tv.ui.components.TvContinueWatchingRow
import com.cineby.tv.ui.components.TvErrorRetry
import com.cineby.tv.ui.components.TvHeroBillboard
import com.cineby.tv.ui.components.TvHeroSkeleton
import com.cineby.tv.ui.components.TvSkeletonRow
import com.cineby.tv.ui.components.TvSwimlaneRow
import com.cineby.tv.ui.theme.TvBackground

@Composable
fun HomeScreen(
    onNavigateToDetail: (contentType: String, slug: String) -> Unit,
    onNavigateToPlayer: (contentType: String, contentId: String, title: String, poster: String?, backdrop: String?, slug: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val heroFocusRequester = remember { FocusRequester() }

    // Refresh continue watching list on screen entrance / return
    LaunchedEffect(Unit) {
        viewModel.refreshContinueWatching()
    }

    // Direct initial focus to Hero billboard content as soon as loaded
    LaunchedEffect(uiState.isLoading, uiState.heroMovie) {
        if (!uiState.isLoading && uiState.heroMovie != null) {
            try {
                heroFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        if (uiState.isLoading) {
            TvLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { TvHeroSkeleton() }
                item { TvSkeletonRow() }
                item { TvSkeletonRow(cardCount = 4) }
            }
        } else if (uiState.error != null) {
            TvErrorRetry(
                message = uiState.error ?: "",
                onRetry = { viewModel.loadHomeData() }
            )
        } else {
            TvLazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Dynamic Hero Billboard
                item {
                    TvHeroBillboard(
                        movie = uiState.heroMovie,
                        playButtonModifier = Modifier.focusRequester(heroFocusRequester),
                        onPlayClick = {
                            uiState.heroMovie?.let {
                                if (it.contentType == "tv_series") {
                                    onNavigateToDetail(it.contentType, it.slug)
                                } else {
                                    onNavigateToPlayer(
                                        it.contentType,
                                        it.id,
                                        it.title,
                                        it.fullPosterUrl,
                                        it.fullBackdropUrl,
                                        it.slug
                                    )
                                }
                            }
                        },
                        onDetailsClick = {
                            uiState.heroMovie?.let {
                                onNavigateToDetail(it.contentType, it.slug)
                            }
                        }
                    )
                }

                // 2. Bar Paling Pertama: Lanjutkan Menonton (hanya muncul jika ada histori yang belum kelar)
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        TvContinueWatchingRow(
                            title = stringResource(R.string.continue_watching),
                            items = uiState.continueWatching,
                            onItemClick = { item ->
                                onNavigateToPlayer(
                                    item.contentType,
                                    item.contentId,
                                    item.title,
                                    item.posterUrl,
                                    item.backdropUrl,
                                    item.slug
                                )
                            }
                        )
                    }
                }

                // 3. Swimlane: Sedang Tren Sekarang
                item {
                    TvSwimlaneRow(
                        title = stringResource(R.string.trending_now),
                        items = uiState.trending,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                // 4. Swimlane: Film Terpopuler
                item {
                    TvSwimlaneRow(
                        title = stringResource(R.string.top_10_movies),
                        items = uiState.popularMovies,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                // 5. Swimlane: Serial TV Pilihan
                item {
                    val seriesAsMovies = remember(uiState.popularSeries) {
                        uiState.popularSeries.map { s ->
                            MovieItem(
                                id = s.id,
                                title = s.title,
                                slug = s.slug,
                                posterPath = s.posterPath,
                                backdropPath = s.backdropPath,
                                voteAverage = s.voteAverage,
                                contentType = "tv_series"
                            )
                        }
                    }
                    TvSwimlaneRow(
                        title = stringResource(R.string.popular_series),
                        items = seriesAsMovies,
                        onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                    )
                }

                // 6. Swimlane: Film Aksi
                if (uiState.actionMovies.isNotEmpty()) {
                    item {
                        TvSwimlaneRow(
                            title = stringResource(R.string.movies_action),
                            items = uiState.actionMovies,
                            onItemClick = { onNavigateToDetail(it.contentType, it.slug) }
                        )
                    }
                }
            }
        }
    }
}
