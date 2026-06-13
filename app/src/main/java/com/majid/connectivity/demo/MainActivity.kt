package com.majid.connectivity.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.majid.connectivity.NetworkStatus
import com.majid.connectivity.compose.rememberConnectivityState
import com.majid.connectivity.demo.ui.theme.ConnectivityTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConnectivityTheme {
                ConnectivityDemoScreen()
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Screen
// ------------------------------------------------------------------------------------------------

@Composable
fun ConnectivityDemoScreen() {
    val status by rememberConnectivityState()

    // Keep a rolling log of status changes
    val history = remember { mutableStateListOf<HistoryEntry>() }
    LaunchedEffect(status) {
        if (status !is NetworkStatus.Unknown) {
            history.add(0, HistoryEntry(status, formattedTime()))
            if (history.size > 20) history.removeAt(history.lastIndex)
        }
    }

    val bgColor by animateColorAsState(
        targetValue = status.backgroundColor(),
        animationSpec = tween(600),
        label = "background",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        // Hero card
        StatusHeroCard(status = status, bgColor = bgColor)

        Spacer(Modifier.height(16.dp))

        // Feature chips row
        FeatureRow(status = status)

        Spacer(Modifier.height(24.dp))

        // History log
        Text(
            text = "Change History",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(history, key = { it.time + it.status.label }) { entry ->
                HistoryItem(entry = entry)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Hero card
// ------------------------------------------------------------------------------------------------

@Composable
private fun StatusHeroCard(status: NetworkStatus, bgColor: Color) {
    val scale by animateFloatAsState(
        targetValue = if (status.isConnected) 1f else 0.92f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AnimatedContent(
                targetState = status.icon(),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "icon",
            ) { icon ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = status.label,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            AnimatedContent(
                targetState = status.label,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "label",
            ) { label ->
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = status.description(),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )

            // Connected indicator dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (status.isConnected) Color(0xFF69F0AE) else Color(0xFFFF5252)),
                )
                Text(
                    text = if (status.isConnected) "Connected" else "Disconnected",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// Feature row
// ------------------------------------------------------------------------------------------------

@Composable
private fun FeatureRow(status: NetworkStatus) {
    val features = listOf(
        Triple("WiFi",     Icons.Rounded.Wifi,       status is NetworkStatus.WiFi),
        Triple("Cellular", Icons.Rounded.CellTower,  status is NetworkStatus.Cellular),
        Triple("VPN",      Icons.Rounded.Lock,       status is NetworkStatus.Vpn),
        Triple("Offline",  Icons.Rounded.CloudOff,   status is NetworkStatus.Offline),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        features.forEach { (label, icon, active) ->
            FeatureChip(
                label = label,
                icon = icon,
                active = active,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun FeatureChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val chipBg by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(400),
        label = "chipBg",
    )
    val contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = chipBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// History
// ------------------------------------------------------------------------------------------------

data class HistoryEntry(val status: NetworkStatus, val time: String)

@Composable
private fun HistoryItem(entry: HistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(entry.status.backgroundColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.status.icon(),
                contentDescription = null,
                tint = entry.status.backgroundColor(),
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.status.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = entry.status.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = entry.time,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ------------------------------------------------------------------------------------------------
// Extensions on NetworkStatus
// ------------------------------------------------------------------------------------------------

@Composable
private fun NetworkStatus.backgroundColor(): Color = when (this) {
    NetworkStatus.WiFi     -> Color(0xFF1565C0)
    NetworkStatus.Cellular -> Color(0xFF2E7D32)
    NetworkStatus.Vpn      -> Color(0xFF6A1B9A)
    NetworkStatus.Offline  -> Color(0xFFB71C1C)
    NetworkStatus.Unknown  -> MaterialTheme.colorScheme.outline
}

private fun NetworkStatus.icon(): ImageVector = when (this) {
    NetworkStatus.WiFi     -> Icons.Rounded.Wifi
    NetworkStatus.Cellular -> Icons.Rounded.CellTower
    NetworkStatus.Vpn      -> Icons.Rounded.Lock
    NetworkStatus.Offline  -> Icons.Rounded.CloudOff
    NetworkStatus.Unknown  -> Icons.Rounded.HelpOutline
}

private fun NetworkStatus.description(): String = when (this) {
    NetworkStatus.WiFi     -> "Connected via wireless network"
    NetworkStatus.Cellular -> "Connected via mobile data"
    NetworkStatus.Vpn      -> "Traffic routed through VPN tunnel"
    NetworkStatus.Offline  -> "No active network connection"
    NetworkStatus.Unknown  -> "Determining network state…"
}

private fun formattedTime(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

// ------------------------------------------------------------------------------------------------
// Preview
// ------------------------------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun PreviewWifi() {
    ConnectivityTheme {
        Surface {
            StatusHeroCard(
                status = NetworkStatus.WiFi,
                bgColor = Color(0xFF1565C0),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOffline() {
    ConnectivityTheme {
        Surface {
            StatusHeroCard(
                status = NetworkStatus.Offline,
                bgColor = Color(0xFFB71C1C),
            )
        }
    }
}
