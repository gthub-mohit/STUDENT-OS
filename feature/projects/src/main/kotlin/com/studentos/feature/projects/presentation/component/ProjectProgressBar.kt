package com.studentos.feature.projects.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ProjectProgressBar — A deterministic linear progress bar where filled width matches
 * the exact percentage from 0.0 to 1.0 without false stop indicators.
 *
 * 0%   -> 0 fill (empty track)
 * 25%  -> 25% filled from left
 * 50%  -> 50% filled from left
 * 75%  -> 75% filled from left
 * 100% -> 100% filled to right
 */
@Composable
fun ProjectProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val normalizedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        if (normalizedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalizedProgress)
                    .clip(RoundedCornerShape(height / 2))
                    .background(color)
            )
        }
    }
}
