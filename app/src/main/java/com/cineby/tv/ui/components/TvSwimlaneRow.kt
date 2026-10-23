package com.cineby.tv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Text
import com.cineby.tv.data.model.MovieItem
import com.cineby.tv.ui.theme.TvOnSurface

@Composable
fun TvSwimlaneRow(
    title: String,
    items: List<MovieItem>,
    modifier: Modifier = Modifier,
    onItemFocus: ((MovieItem) -> Unit)? = null,
    onItemClick: (MovieItem) -> Unit
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = title,
            color = TvOnSurface,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp)
        )

        TvLazyRow(
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 6.dp),
            pivotOffsets = PivotOffsets(parentFraction = 0.15f, childFraction = 0f)
        ) {
            items(items, key = { it.id }) { movie ->
                TvMovieCard(
                    title = movie.title,
                    posterUrl = movie.fullPosterUrl,
                    rating = movie.formattedRating,
                    quality = movie.quality,
                    modifier = Modifier.padding(end = 12.dp),
                    onFocus = { onItemFocus?.invoke(movie) },
                    onClick = { onItemClick(movie) }
                )
            }
        }
    }
}
