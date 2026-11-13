package com.rescue.mesh.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.hardware.GpsLocationManager
import com.rescue.mesh.android.ui.theme.*

/**
 * Huy hi\u1ec7u hi\u1ec3n th\u1ecb tr\u1ea1ng th\u00e1i v\u00e0 t\u1ecda \u0111\u1ed9 GPS V\u1ec7 tinh th\u1ef1c t\u1ebf.
 *
 * Code m\u00e0u tr\u1ea1ng th\u00e1i:
 *   - Xanh l\u00e1: Sai s\u1ed1 \u2264 10m (GPS ch\u00ednh x\u00e1c cao)
 *   - V\u00e0ng: Sai s\u1ed1 \u2264 30m (GPS t\u1ea1m ch\u1ea5p nh\u1eadn)
 *   - Cam: Sai s\u1ed1 > 30m (GPS y\u1ebfu)
 *   - \u0110\u1ecf: Kh\u00f4ng c\u00f3 t\u00edn hi\u1ec7u GPS
 */
@Composable
fun LiveGpsBadge(
    location: GpsLocationManager.LocationData?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gps_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gps_dot"
    )

    val statusColor = when {
        location == null -> CriticalRed
        location.accuracy <= 10f -> ConnectedGreen
        location.accuracy <= 30f -> WarningYellow
        else -> HighAmber
    }

    TacticalCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = DarkElevated.copy(alpha = 0.85f),
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (location != null) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    contentDescription = "GPS",
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (location != null) "GPS V\u1ec6 TINH TR\u1ef0C TI\u1ebeP" else "\u0110ANG T\u00ccM T\u00cdN HI\u1ec6U GPS...",
                        style = Typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                    if (location != null) {
                        Text(
                            text = String.format("%.6f, %.6f", location.latitude, location.longitude),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    } else {
                        Text(
                            text = "\u0110ang b\u1eaft s\u00f3ng v\u1ec7 tinh...",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (location != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sai s\u1ed1",
                        style = Typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "\u00b1${location.accuracy.toInt()}m",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = statusColor
                    )
                }
            }
        }
    }
}