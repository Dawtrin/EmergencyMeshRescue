package com.rescue.mesh.android.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * N\u00fat Hero SOS Tr\u1ecdng T\u00e2m v\u1edbi Hi\u1ec7u \u1ee9ng S\u00f3ng Radar T\u1ecfa Nhi\u1ec7t 3 T\u1ea7ng.
 *
 * Thi\u1ebft k\u1ebf:
 *   - \u0110\u01b0\u1eddng k\u00ednh 180dp (theo chu\u1ea9n SUPER_PROMPT).
 *   - 3 v\u00f2ng tr\u00f2n s\u00f3ng Radar lan t\u1ecfa v\u00f4 t\u1eadn (Infinite Ripple Pulse).
 *   - C\u01a1 ch\u1ebf nh\u1ea5n gi\u1eef 1.5 gi\u00e2y v\u1edbi v\u00f2ng tr\u00f2n ti\u1ebfn tr\u00ecnh quang h\u1ecdc.
 *   - Spring bounce animation khi nh\u1ea5n v\u00e0 th\u1ea3 n\u00fat.
 *   - Rung x\u00fac gi\u00e1c \u0111a t\u1ea7ng (Emergency Heartbeat: m\u1ea1nh - v\u1eeba - m\u1ea1nh).
 *   - N\u00fat SOS Nhanh 1-Ch\u1ea1m b\u00ean d\u01b0\u1edbi cho t\u00ecnh hu\u1ed1ng t\u00ednh b\u1eb1ng gi\u00e2y.
 */
@Composable
fun PulseSosButton(
    isBroadcasting: Boolean,
    onSosTriggered: () -> Unit,
    onQuickSos: () -> Unit = onSosTriggered,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    // Spring bounce scale animation
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sos_bounce"
    )

    // 3 v\u00f2ng Radar Pulse animation (l\u1ec7ch pha 0ms, 600ms, 1200ms)
    val infiniteTransition = rememberInfiniteTransition(label = "radar_3ring")

    val pulse1Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "p1_scale"
    )
    val pulse1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "p1_alpha"
    )

    val pulse2Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 600, easing = LinearEasing), RepeatMode.Restart),
        label = "p2_scale"
    )
    val pulse2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 600, easing = LinearEasing), RepeatMode.Restart),
        label = "p2_alpha"
    )

    val pulse3Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 2.0f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "p3_scale"
    )
    val pulse3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(2200, delayMillis = 1200, easing = LinearEasing), RepeatMode.Restart),
        label = "p3_alpha"
    )

    fun triggerEmergencyHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vib = mgr.defaultVibrator
                val timings = longArrayOf(0, 120, 80, 100, 80, 200)
                val amplitudes = intArrayOf(0, 255, 0, 180, 0, 255)
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val timings = longArrayOf(0, 120, 80, 100, 80, 200)
                val amplitudes = intArrayOf(0, 255, 0, 180, 0, 255)
                vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        } catch (_: Exception) {}
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // === 3 V\u00d2NG RADAR PULSE ===
            Box(modifier = Modifier.size(180.dp * pulse3Scale).clip(CircleShape)
                .background(CriticalRedGlow.copy(alpha = pulse3Alpha)))
            Box(modifier = Modifier.size(180.dp * pulse2Scale).clip(CircleShape)
                .background(CriticalRed.copy(alpha = pulse2Alpha * 0.6f)))
            Box(modifier = Modifier.size(180.dp * pulse1Scale).clip(CircleShape)
                .background(CriticalRed.copy(alpha = pulse1Alpha)))

            // === CIRCULAR HOLD PROGRESS ===
            Canvas(modifier = Modifier.size(200.dp)) {
                if (holdProgress > 0f) {
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * holdProgress,
                        useCenter = false,
                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // === MAIN CORE SOS BUTTON (180dp) ===
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(buttonScale)
                    .shadow(28.dp, CircleShape, spotColor = CriticalRed, ambientColor = CriticalRed)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF6B5E),
                                CriticalRed,
                                Color(0xFFCC1F15)
                            )
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                holdProgress = 0f
                                val holdJob = scope.launch {
                                    val totalTime = 1500L
                                    val step = 25L
                                    var elapsed = 0L
                                    while (elapsed < totalTime && isPressed) {
                                        delay(step)
                                        elapsed += step
                                        holdProgress = (elapsed.toFloat() / totalTime).coerceIn(0f, 1f)
                                    }
                                    if (holdProgress >= 1f) {
                                        triggerEmergencyHaptic()
                                        onSosTriggered()
                                        holdProgress = 0f
                                    }
                                }
                                tryAwaitRelease()
                                isPressed = false
                                holdJob.cancel()
                                holdProgress = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SOS",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        color = Color.White
                    )
                    Text(
                        text = if (isPressed) "GI\u1eee \u0110\u1ebe PH\u00c1T" else "NH\u1ea4N GI\u1eee 1.5S",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === N\u00daTSOS NHANH 1-CH\u1ea0M ===
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(CriticalRed.copy(alpha = 0.15f))
                .clickable {
                    triggerEmergencyHaptic()
                    onQuickSos()
                }
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Quick SOS",
                    tint = CriticalRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SOS NHANH 1-CH\u1ea0M",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = CriticalRed
                )
            }
        }
    }
}