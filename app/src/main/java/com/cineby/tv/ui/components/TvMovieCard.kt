package com.cineby.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.cineby.tv.ui.theme.TvSurface
import com.cineby.tv.ui.theme.TvGold
import com.cineby.tv.ui.theme.TvMuted
import com.cineby.tv.ui.theme.TvBrand
import com.cineby.tv.ui.theme.TvOnSurface

private val BottomScrimBrush = Brush.verticalGradient(
    colors = listOf(Color.Transparent, Color(0xEE141414))
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMovieCard(
    title: String,
    posterUrl: String,
    rating: String,
    quality: String? = null,
    modifier: Modifier = Modifier,
    width: Int = 135,
    height: Int = 200,
    onFocus: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(posterUrl) {
        ImageRequest.Builder(context)
            .data(posterUrl)
            .size(270, 400)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(width.dp)
            .height(height.dp)
            .onFocusChanged {
                if (it.isFocused) {
                    onFocus?.invoke()
                }
            },
        scale = tvCardScale(),
        glow = tvCardGlow(),
        shape = tvCardShape(),
        border = tvCardBorder()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvSurface)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient scrim at bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(BottomScrimBrush)
            )

            // Bottom-left quality badge (if any)
            if (!quality.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(TvBrand.copy(alpha = 0.9f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quality,
                        color = TvOnSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom title & rating
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = title,
                    color = TvOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (rating.isNotBlank() && rating != "N/A") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = TvGold,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = rating,
                            color = TvMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
