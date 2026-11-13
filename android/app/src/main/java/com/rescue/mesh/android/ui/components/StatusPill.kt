package com.rescue.mesh.android.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.ui.theme.*

/**
 * Vi\u00ean thu\u1ed1c tr\u1ea1ng th\u00e1i Ma thu\u1eadt (Dynamic Island / Capsule Pill).
 *
 * T\u1ef1 \u0111\u1ed9ng co gi\u00e3n \u0111\u00e0n h\u1ed3i ki\u1ec3u Dynamic Island khi tr\u1ea1ng th\u00e1i thay \u0111\u1ed5i
 * (VICTIM <-> RELAY, s\u1ed1 node t\u0103ng gi\u1ea3m) v\u1edbi Spring Physics animation.
 */
@Composable
fun StatusPill(
    nodeMode: String,
    neighborCount: Int,
    isRelayRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val pillBgColor by animateColorAsState(
        targetValue = if (isRelayRunning) RelayPurple.copy(alpha = 0.2f) else DarkElevated.copy(alpha = 0.9f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pill_bg"
    )

    val activeColor = if (isRelayRunning) RelayPurple else ConnectedGreen

    Box(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clip(CircleShape)
            .background(pillBgColor)
            .border(1.dp, activeColor.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = "Mesh",
                tint = activeColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$nodeMode \u2022 $neighborCount NODES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TextPrimary
            )
        }
    }
}