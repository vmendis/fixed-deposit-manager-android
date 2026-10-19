package com.example.fdmanager.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fdmanager.domain.SyncState

/**
 * #3 Sync-state chip — small persistent indicator: Saved / Syncing / Offline / Error
 * Mock only, 0 quota. Phase 2 will swap FakeSyncRepository to FirebaseSyncRepository without layout changes.
 * Gate: syncstate-0300r9
 */
@Composable
fun SyncStateChip(
    state: SyncState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bg, fg, icon) = when (state) {
        SyncState.SAVED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.CheckCircle
        )
        SyncState.SYNCING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Filled.Sync
        )
        SyncState.OFFLINE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Filled.CloudOff
        )
        SyncState.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Error
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .testTag("syncstate-0300r9")
            .then(if (onClick != null) Modifier else Modifier)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == SyncState.SYNCING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = fg
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = state.label,
                    modifier = Modifier.size(14.dp),
                    tint = fg
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                modifier = Modifier.testTag("syncStateLabel")
            )
        }
    }
}

@Composable
fun SyncStateChipWithDescription(
    state: SyncState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag("syncstate-0300r9"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SyncStateChip(state = state)
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("syncStateDescription")
        )
    }
}
