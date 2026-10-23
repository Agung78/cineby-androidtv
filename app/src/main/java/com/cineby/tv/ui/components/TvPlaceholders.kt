package com.cineby.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.cineby.tv.R
import com.cineby.tv.ui.theme.TvBrand
import com.cineby.tv.ui.theme.TvMuted
import com.cineby.tv.ui.theme.TvOnSurface
import com.cineby.tv.ui.theme.TvSurface

@Composable
fun TvSkeletonRow(modifier: Modifier = Modifier, cardCount: Int = 6) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Box(
            modifier = Modifier.padding(start = 48.dp, bottom = 10.dp).size(140.dp, 18.dp)
                .background(TvSurface, RoundedCornerShape(4.dp))
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            repeat(cardCount) {
                Box(
                    modifier = Modifier.size(135.dp, 200.dp)
                        .background(TvSurface, RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvErrorRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val retryRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { retryRequester.requestFocus() } catch (_: Exception) {}
    }
    Column(
        modifier = modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = TvMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryRequester),
            colors = ButtonDefaults.colors(
                containerColor = TvBrand,
                contentColor = TvOnSurface,
                focusedContainerColor = TvOnSurface,
                focusedContentColor = TvBrand
            ),
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(6.dp))
        ) {
            Text(
                text = stringResource(R.string.try_again),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TvHeroSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(420.dp)
            .background(TvSurface)
    )
}
