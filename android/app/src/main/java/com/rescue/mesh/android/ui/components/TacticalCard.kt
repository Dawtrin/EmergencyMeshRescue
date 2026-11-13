package com.rescue.mesh.android.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rescue.mesh.android.ui.theme.GlassSurface

/**
 * Th\u1ebb k\u00ednh m\u1edd phong c\u00e1ch Neo-iOS Visionary (Glassmorphism 2.0 & Specular Depth).
 *
 * \u0110\u1eb7c \u0111i\u1ec3m thi\u1ebft k\u1ebf:
 *   - N\u1ec1n b\u00e1n trong su\u1ed1t rgba(28, 28, 30, 0.72) t\u1ea1o hi\u1ec7u \u1ee9ng Frosted Glass.
 *   - Vi\u1ec1n s\u00e1ng ph\u1ea3n quang Specular Gradient t\u1eeb tr\u00ean xu\u1ed1ng (White 18% -> Transparent).
 *   - \u0110\u1ed5 b\u00f3ng chi\u1ec1u s\u00e2u \u0111a l\u1edbp (elevation 12dp, black 50%).
 *   - Bo g\u00f3c m\u1ec1m m\u1ea1i 18dp.
 *   - T\u1ea5t c\u1ea3 animation d\u00f9ng Spring Physics (DampingRatioMediumBouncy).
 */
@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    backgroundColor: Color = GlassSurface,
    borderColor: Color = Color.White.copy(alpha = 0.18f),
    elevation: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    // Specular Border Highlight: Gradient ph\u1ea3n x\u1ea1 \u00e1nh s\u00e1ng tr\u00ean m\u1eb7t k\u00ednh v\u00e1t kim c\u01b0\u01a1ng
    val specularBrush = Brush.linearGradient(
        colors = listOf(
            borderColor,
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(shape)
            .background(backgroundColor, shape)
            .border(BorderStroke(1.dp, specularBrush), shape)
            .padding(16.dp),
        content = content
    )
}