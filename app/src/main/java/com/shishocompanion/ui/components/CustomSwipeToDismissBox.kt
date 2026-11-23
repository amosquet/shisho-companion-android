package com.shishocompanion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shishocompanion.R
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSwipeToDismissBox(
    swipeActionPreference: String,
    onDismiss: () -> Unit,
    onArchive: () -> Unit,
    content: @Composable () -> Unit
) {
    val isEnabled = swipeActionPreference != "NONE"
    val haptic = LocalHapticFeedback.current
    var boxWidth by remember { mutableIntStateOf(0) }
    var stateRef by remember { mutableStateOf<SwipeToDismissBoxState?>(null) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            if (!isEnabled || targetValue != SwipeToDismissBoxValue.EndToStart) {
                targetValue == SwipeToDismissBoxValue.Settled
            } else {
                // Reject fast accidental flicks unless dragged at least 45% of card width
                val currentOffset = try {
                    abs(stateRef?.requireOffset() ?: 0f)
                } catch (_: IllegalStateException) {
                    0f
                }
                boxWidth > 0 && currentOffset >= boxWidth * 0.45f
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.55f }
    )
    SideEffect { stateRef = dismissState }

    val pastThreshold by remember {
        derivedStateOf {
            val offset = try {
                abs(dismissState.requireOffset())
            } catch (_: IllegalStateException) {
                0f
            }
            boxWidth > 0 && offset >= boxWidth * 0.45f
        }
    }

    LaunchedEffect(pastThreshold) {
        if (pastThreshold && isEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            if (swipeActionPreference == "ARCHIVE") {
                onArchive()
            } else if (swipeActionPreference != "NONE") {
                onDismiss()
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier.onSizeChanged { boxWidth = it.width },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = isEnabled,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when {
                    direction == SwipeToDismissBoxValue.Settled -> Color.Transparent
                    !pastThreshold -> MaterialTheme.colorScheme.surfaceVariant
                    swipeActionPreference == "ARCHIVE" -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            )
            val icon = if (swipeActionPreference == "ARCHIVE") Icons.Default.Archive else Icons.Default.Delete
            val iconScale by animateFloatAsState(if (pastThreshold) 1.2f else 0.85f)
            val iconTint = when {
                !pastThreshold -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                swipeActionPreference == "ARCHIVE" -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onErrorContainer
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = icon,
                        contentDescription = stringResource(
                            if (swipeActionPreference == "ARCHIVE") R.string.archive else R.string.delete
                        ),
                        tint = iconTint,
                        modifier = Modifier.scale(iconScale)
                    )
                }
            }
        },
        content = { content() }
    )
}
