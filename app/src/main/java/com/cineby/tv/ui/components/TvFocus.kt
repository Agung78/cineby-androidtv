package com.cineby.tv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import com.cineby.tv.ui.theme.TvBrand

private val CardShape = RoundedCornerShape(8.dp)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun tvCardScale() = CardDefaults.scale(focusedScale = 1.1f)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun tvCardGlow() = CardDefaults.glow(focusedGlow = Glow(TvBrand.copy(alpha = 0.35f), 8.dp))

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun tvCardBorder() = CardDefaults.border(
    focusedBorder = Border(border = BorderStroke(2.dp, Color.White))
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun tvCardShape() = CardDefaults.shape(shape = CardShape)

fun Modifier.tvFocusElevation(isFocused: Boolean, elevation: Dp = 8.dp): Modifier =
    shadow(if (isFocused) elevation else 0.dp, CardShape)
