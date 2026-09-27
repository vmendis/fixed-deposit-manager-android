package com.example.fdmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

/**
 * Rounded monogram tile for a bank: brand-hue background + short initials.
 * Replaces the generic bank icon (roadmap #18). Renders no logo artwork —
 * identity comes from a config color and letters, so there is nothing
 * copyrighted in the repo. Unknown banks get a neutral fallback tile.
 *
 * @param bank bank name as stored on the deposit (matched via [BankRegistry])
 * @param size tile edge; text scales with it
 */
@Composable
fun BankMonogram(bank: String, size: Dp, modifier: Modifier = Modifier) {
    val identity = remember(bank) { BankRegistry.resolve(bank) }
    val fontSize = (if (identity.code.length <= 2) 0.38f else 0.30f) * size.value

    Box(
        modifier = modifier
            .testTag("bankMonogram")
            .size(size)
            .clip(RoundedCornerShape(percent = 28))
            .background(Color(identity.colorArgb)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = identity.code,
            color = Color(BankRegistry.contentColorFor(identity)),
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
