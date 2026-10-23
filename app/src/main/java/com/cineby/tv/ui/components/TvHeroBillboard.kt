package com.cineby.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.cineby.tv.R
import com.cineby.tv.data.model.MovieItem
import com.cineby.tv.ui.theme.TvBackground
import com.cineby.tv.ui.theme.TvGold
import com.cineby.tv.ui.theme.TvMuted
import com.cineby.tv.ui.theme.TvBrand
import com.cineby.tv.ui.theme.TvOnSurface

private val HeroBottomBrush = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        TvBackground.copy(alpha = 0.5f),
        TvBackground
    ),
    startY = 80f
)

private val HeroLeftBrush = Brush.horizontalGradient(
    colors = listOf(
        TvBackground.copy(alpha = 0.95f),
        TvBackground.copy(alpha = 0.6f),
        Color.Transparent
    )
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHeroBillboard(
    movie: MovieItem?,
    modifier: Modifier = Modifier,
    playButtonModifier: Modifier = Modifier,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    if (movie == null) return

    val context = LocalContext.current
    val backdropUrl = movie.fullBackdropUrl
    val backdropRequest = remember(backdropUrl) {
        ImageRequest.Builder(context)
            .data(backdropUrl)
            .size(1280, 720)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        // Dynamic Backdrop
        AsyncImage(
            model = backdropRequest,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom gradient fade to black
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HeroBottomBrush)
        )

        // Left gradient fade ONLY on text area (saves 70% fillrate)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(540.dp)
                .background(HeroLeftBrush)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(560.dp)
                .padding(start = 48.dp, top = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Title
            Text(
                text = movie.title,
                color = TvOnSurface,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Badges (Rating, Year, Quality)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (movie.formattedRating != "N/A") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = TvGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${movie.formattedRating} / 10",
                            color = TvOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                movie.releaseDate?.take(4)?.let { year ->
                    Text(
                        text = year,
                        color = TvMuted,
                        fontSize = 12.sp
                    )
                }

                movie.quality?.let { q ->
                    Box(
                        modifier = Modifier
                            .background(Color(0x44FFFFFF), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = q,
                            color = TvOnSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            val metaLine = buildList {
                movie.releaseDate?.take(4)?.let { add(it) }
                movie.genres.take(3).forEach { add(it.name) }
                movie.quality?.let { add(it.uppercase()) }
            }.joinToString("  ·  ")
            if (metaLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = metaLine,
                    color = TvMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Synopsis
            if (!movie.overview.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = movie.overview!!,
                    color = TvOnSurface.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(520.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = playButtonModifier,
                    colors = ButtonDefaults.colors(
                        containerColor = TvOnSurface,
                        contentColor = TvBackground,
                        focusedContainerColor = TvBrand,
                        focusedContentColor = TvOnSurface
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.play),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.play),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = onDetailsClick,
                    colors = ButtonDefaults.colors(
                        containerColor = Color(0x66FFFFFF),
                        contentColor = TvOnSurface,
                        focusedContainerColor = TvOnSurface,
                        focusedContentColor = TvBackground
                    ),
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.more_info),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.more_info),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
