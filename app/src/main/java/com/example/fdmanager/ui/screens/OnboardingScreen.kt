package com.example.fdmanager.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vssbox.fdmanager.R

// ONBOARDING_GATE = onboarding-0100r9 — freshness marker for V24 #27
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onExploreDemo: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("onboardingScreen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))

            // vssbox cube branding — 96dp, transparent PNG from docs/branding/vssbox_logo_transparent_manual.png
            Image(
                painter = painterResource(id = R.drawable.vssbox_cube),
                contentDescription = "vssbox logo",
                modifier = Modifier
                    .size(96.dp)
                    .testTag("vssboxLogoOnboarding")
            )
            Spacer(Modifier.height(16.dp))

            Text(
                text = "vssbox",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("vssboxNameOnboarding")
            )
            Spacer(Modifier.height(4.dp))

            Text(
                text = "FD Manager",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "by vssbox • Secure box for your deposits",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(32.dp))

            // Trust line from #1
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trustCard")
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Your data stays yours",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "We never ask for your bank login. Your data is encrypted and only you can see it. Runs entirely on local sample data in this mock — no account, no backend, no real money.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Features — 3 bullets
            FeatureRow(
                icon = Icons.Filled.TrackChanges,
                title = "Track all FDs in one place",
                subtitle = "Total invested, maturing soon, by institution — CBSL-regulated banks only (24 LCB + 6 LSB + 32 LFC)"
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                icon = Icons.Filled.Savings,
                title = "Rate • Tenor • Payout • Matures",
                subtitle = "10% p.a. • 12 months • Monthly payout • matures 16 Oct 2026 — built for Sri Lankan FDs"
            )
            Spacer(Modifier.height(12.dp))
            FeatureRow(
                icon = Icons.Filled.Lock,
                title = "Renewal chain & Recycle bin",
                subtitle = "Renew with linked child, soft-delete to bin, restore anytime. Demo mode toggle in Settings."
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("getStartedButton")
            ) {
                Text("Get Started — Add your first FD", fontWeight = FontWeight.Bold)
            }
            if (onExploreDemo != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onExploreDemo,
                    modifier = Modifier.testTag("exploreDemoButton")
                ) {
                    Text("Explore demo data first")
                }
            }
            Spacer(Modifier.height(16.dp))
            // Footer branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("builtByVssboxFooter")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vssbox_cube),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Built by vssbox • © 2026 vssbox",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
