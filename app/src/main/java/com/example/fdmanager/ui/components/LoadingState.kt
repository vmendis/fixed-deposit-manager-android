package com.example.fdmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// LOADSTATE_GATE = loadstate-0400r9 — freshness marker for #4
const val LOADSTATE_GATE = "loadstate-0400r9"

/**
 * #4 Shared loading & error patterns
 * One implementation used by Home, list, detail, calendar
 * Toggled by debug switch in Settings for review
 */

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isLoading) brush else MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun LoadingSkeletonCard(
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("loadingSkeletonCard")
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(Modifier.size(46.dp).clip(RoundedCornerShape(50)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    ShimmerBox(Modifier.fillMaxWidth(0.6f).height(16.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.8f).height(14.dp))
            Spacer(Modifier.height(6.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.5f).height(12.dp))
        }
    }
}

@Composable
fun LoadingList(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("loadingList")
            .testTag(LOADSTATE_GATE)
    ) {
        repeat(count) {
            LoadingSkeletonCard()
        }
    }
}

@Composable
fun FullScreenLoading(
    message: String = "Loading FDs...",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp)
            .testTag("fullScreenLoading")
            .testTag(LOADSTATE_GATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp).testTag("loadingSpinner"),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("loadingMessage")
        )
    }
}

@Composable
fun FullScreenError(
    title: String = "Something went wrong",
    message: String = "Failed to load FDs (mock) — check your connection",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp)
            .testTag("fullScreenError")
            .testTag(LOADSTATE_GATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp).testTag("errorIcon"),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("errorTitle")
        )
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("errorMessage")
        )
        if (onRetry != null) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.testTag("errorRetryButton")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry")
            }
        }
    }
}

@Composable
fun InlineError(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("inlineError"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).testTag("inlineErrorMessage")
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry, modifier = Modifier.testTag("inlineErrorRetry")) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun LoadingErrorContainer(
    isLoading: Boolean,
    isError: Boolean,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    loadingContent: @Composable () -> Unit = { FullScreenLoading() },
    errorContent: @Composable () -> Unit = {
        FullScreenError(
            message = errorMessage ?: "Failed to load (mock)",
            onRetry = onRetry
        )
    },
    content: @Composable () -> Unit
) {
    when {
        isLoading -> loadingContent()
        isError -> errorContent()
        else -> content()
    }
}
