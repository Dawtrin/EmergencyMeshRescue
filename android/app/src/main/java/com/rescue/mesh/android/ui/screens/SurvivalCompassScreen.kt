package com.rescue.mesh.android.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.repository.MeshRepository
import com.rescue.mesh.android.hardware.CompassSensorManager
import com.rescue.mesh.android.hardware.GpsLocationManager
import com.rescue.mesh.android.ui.components.TacticalCard
import com.rescue.mesh.android.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Màn hình La bàn Sinh tồn & Radar Tác chiến Dã chiến.
 *
 * Hỗ trợ 2 chế độ xem:
 *   1. LA BÀN TỪ TÍNH: Kim la bàn chỉ hướng Bắc + vector hướng về Trạm cứu hộ.
 *   2. BẢN ĐỒ RADAR DÃ CHIẾN: Lưới đo cự ly đa tầng (250m, 500m, 1km, 2km) + chớp quét sóng Radar.
 */
@Composable
fun SurvivalCompassScreen(
    repository: MeshRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val compassManager = remember { CompassSensorManager(context) }

    var isRadarMode by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GpsLocationManager.LocationData?>(null) }
    var compassState by remember { mutableStateOf(CompassSensorManager.CompassState(0f, 0f, 0f)) }

    val baseStationLat = 16.054407
    val baseStationLng = 108.202167

    LaunchedEffect(Unit) {
        repository.gpsManager.getLocationUpdates().collect { loc -> currentLocation = loc }
    }
    LaunchedEffect(currentLocation) {
        val lat = currentLocation?.latitude ?: baseStationLat
        val lng = currentLocation?.longitude ?: baseStationLng
        compassManager.getCompassUpdates(baseStationLat, baseStationLng, lat, lng).collect { compassState = it }
    }

    val relativeAngle = (compassState.bearingToBaseStation - compassState.azimuthDegrees + 360f) % 360f

    // Radar scan beam rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )

    val directionLabel = when {
        compassState.bearingToBaseStation in 337.5f..360f || compassState.bearingToBaseStation in 0f..22.5f -> "H\u01b0\u1edbng B\u1eafc"
        compassState.bearingToBaseStation in 22.5f..67.5f -> "H\u01b0\u1edbng \u0110\u00f4ng B\u1eafc"
        compassState.bearingToBaseStation in 67.5f..112.5f -> "H\u01b0\u1edbng \u0110\u00f4ng"
        compassState.bearingToBaseStation in 112.5f..157.5f -> "H\u01b0\u1edbng \u0110\u00f4ng Nam"
        compassState.bearingToBaseStation in 157.5f..202.5f -> "H\u01b0\u1edbng Nam"
        compassState.bearingToBaseStation in 202.5f..247.5f -> "H\u01b0\u1edbng T\u00e2y Nam"
        compassState.bearingToBaseStation in 247.5f..292.5f -> "H\u01b0\u1edbng T\u00e2y"
        compassState.bearingToBaseStation in 292.5f..337.5f -> "H\u01b0\u1edbng T\u00e2y B\u1eafc"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigateBack() },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(DarkElevated)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("LA B\u00c0N SINH T\u1ed2N", style = Typography.titleLarge.copy(fontWeight = FontWeight.Black))
                        Text("\u0110\u1ecbnh h\u01b0\u1edbng t\u1edbi Tr\u1ea1m C\u1ee9u H\u1ed9", style = Typography.bodyMedium)
                    }
                }

                // Mode switch button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRadarMode) ConnectedGreen.copy(alpha = 0.25f) else DarkElevated)
                        .clickable { isRadarMode = !isRadarMode }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isRadarMode) Icons.Default.Radar else Icons.Default.Explore,
                            contentDescription = null,
                            tint = if (isRadarMode) ConnectedGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRadarMode) "RADAR" else "LA B\u00c0N",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRadarMode) ConnectedGreen else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Canvas Compass / Radar
            Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 20.dp.toPx()

                    if (isRadarMode) {
                        // === BẢN ĐỒ RADAR DÃ CHIẾN (4 VÒNG CỰ LY: 250m, 500m, 1km, 2km) ===
                        val rings = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                        rings.forEach { factor ->
                            drawCircle(
                                color = ConnectedGreen.copy(alpha = 0.2f),
                                radius = radius * factor,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Trục tọa độ Crosshairs
                        drawLine(
                            color = ConnectedGreen.copy(alpha = 0.25f),
                            start = Offset(center.x - radius, center.y),
                            end = Offset(center.x + radius, center.y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = ConnectedGreen.copy(alpha = 0.25f),
                            start = Offset(center.x, center.y - radius),
                            end = Offset(center.x, center.y + radius),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Radar sweep beam line
                        val sweepRad = Math.toRadians(sweepAngle.toDouble())
                        val sweepEnd = Offset(
                            center.x + radius * cos(sweepRad).toFloat(),
                            center.y + radius * sin(sweepRad).toFloat()
                        )
                        drawLine(
                            color = ConnectedGreen.copy(alpha = 0.6f),
                            start = center,
                            end = sweepEnd,
                            strokeWidth = 2.dp.toPx()
                        )

                        // Base station blip dot
                        val baseRad = Math.toRadians((relativeAngle - 90).toDouble())
                        val blipDist = (radius * 0.7f).coerceAtMost(radius)
                        val blipPos = Offset(
                            center.x + blipDist * cos(baseRad).toFloat(),
                            center.y + blipDist * sin(baseRad).toFloat()
                        )
                        drawCircle(color = CriticalRed, radius = 7.dp.toPx(), center = blipPos)
                        drawCircle(color = CriticalRed.copy(alpha = 0.4f), radius = 14.dp.toPx(), center = blipPos)

                        // Center victim blip
                        drawCircle(color = ConnectedGreen, radius = 5.dp.toPx(), center = center)

                    } else {
                        // === LA BÀN TỪ TÍNH TRUYỀN THỐNG ===
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        for (angle in 0 until 360 step 30) {
                            val rad = Math.toRadians((angle - compassState.azimuthDegrees - 90).toDouble())
                            val tickLen = if (angle % 90 == 0) 18.dp.toPx() else 10.dp.toPx()
                            val sx = center.x + (radius - tickLen) * cos(rad).toFloat()
                            val sy = center.y + (radius - tickLen) * sin(rad).toFloat()
                            val ex = center.x + radius * cos(rad).toFloat()
                            val ey = center.y + radius * sin(rad).toFloat()
                            drawLine(
                                color = if (angle == 0) CriticalRed else Color.White.copy(alpha = 0.4f),
                                start = Offset(sx, sy),
                                end = Offset(ex, ey),
                                strokeWidth = if (angle % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                            )
                        }

                        rotate(degrees = relativeAngle, pivot = center) {
                            val arrowPath = Path().apply {
                                moveTo(center.x, center.y - radius + 15.dp.toPx())
                                lineTo(center.x + 16.dp.toPx(), center.y + 20.dp.toPx())
                                lineTo(center.x, center.y + 10.dp.toPx())
                                lineTo(center.x - 16.dp.toPx(), center.y + 20.dp.toPx())
                                close()
                            }
                            drawPath(arrowPath, color = ConnectedGreen)
                        }

                        drawCircle(color = ConnectedGreen, radius = 6.dp.toPx(), center = center)
                    }
                }

                if (!isRadarMode) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${compassState.azimuthDegrees.toInt()}\u00b0", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("H\u01af\u1edaNG B\u1eaeC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rangefinder Card
            TacticalCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "KHO\u1ea2NG C\u00c1CH TR\u1ea0M C\u1ee8U H\u1ed8",
                                style = Typography.labelSmall.copy(color = ConnectedGreen, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val distKm = compassState.distanceMeters / 1000.0
                            Text(
                                text = if (distKm > 0.1) String.format("%.2f km", distKm) else "${compassState.distanceMeters.toInt()} m",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ConnectedGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(directionLabel, fontWeight = FontWeight.Black, fontSize = 12.sp, color = ConnectedGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "T\u1ecda \u0111\u1ed9 m\u1ee5c ti\u00eau: $baseStationLat, $baseStationLng",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}