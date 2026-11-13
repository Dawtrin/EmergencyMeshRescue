package com.rescue.mesh.android.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.local.UserPreferences
import com.rescue.mesh.android.data.repository.MeshRepository
import com.rescue.mesh.android.service.MeshRelayService
import com.rescue.mesh.android.ui.components.StatusPill
import com.rescue.mesh.android.ui.components.TacticalCard
import com.rescue.mesh.android.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Màn hình giám sát chế độ Tiếp sóng (RELAY Monitor).
 */
@Composable
fun RelayMonitorScreen(
    repository: MeshRepository,
    onNavigateToSos: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val stats by repository.networkStats.collectAsState()
    val p2pState by repository.wifiDirectManager.p2pState.collectAsState()
    val profile by repository.userPreferences.userProfileFlow.collectAsState(initial = UserPreferences.UserProfile())

    var isServiceRunning by remember { mutableStateOf(false) }

    val grayscaleMatrix = remember {
        ColorMatrix(floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f,    0f,    0f,    1f, 0f
        ))
    }

    fun toggleRelayService() {
        val intent = Intent(context, MeshRelayService::class.java)
        if (!isServiceRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
            isServiceRunning = true
        } else {
            context.stopService(intent)
            isServiceRunning = false
        }
    }

    val grayscaleModifier = if (profile.ultraBatterySaver) {
        Modifier.drawWithContent {
            val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(grayscaleMatrix) }
            drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
            drawContent()
            drawContext.canvas.restore()
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (profile.ultraBatterySaver) Color.Black else OledBlack)
            .then(grayscaleModifier)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(nodeMode = "TI\u1ebeP S\u00d3NG", neighborCount = p2pState.discoveredPeers.size, isRelayRunning = isServiceRunning)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onNavigateToHistory() },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(DarkElevated)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onNavigateToSos() },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(DarkElevated)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Back", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Relay toggle
            TacticalCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("TI\u1ebeP S\u00d3NG CH\u1ea0Y NG\u1ea6M", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = RelayPurple))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isServiceRunning) "\u0110ang ti\u1ebfp s\u00f3ng li\u00ean t\u1ee5c (Heartbeat 30s)" else "D\u1ecbch v\u1ee5 \u0111ang t\u1ea1m d\u1eebng",
                            fontSize = 13.sp,
                            color = if (isServiceRunning) ConnectedGreen else TextSecondary
                        )
                    }
                    Switch(
                        checked = isServiceRunning,
                        onCheckedChange = { toggleRelayService() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RelayPurple)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ultra Battery Saver
            TacticalCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SI\u00caU TI\u1ebeT KI\u1ec6M PIN SINH T\u1ed2N", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HighAmber))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "T\u1eaft animation, chuy\u1ec3n \u0110en-Tr\u1eafng, gi\u00e3n GPS 3 ph\u00fat/l\u1ea7n. Duy tr\u00ec t\u1eeb 48h-72h.",
                            fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = profile.ultraBatterySaver,
                        onCheckedChange = { enabled -> scope.launch { repository.userPreferences.setUltraBatterySaver(enabled) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = HighAmber)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TacticalCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${stats.packetsForwarded}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = RelayPurple)
                        Text("TI\u1ebeP S\u00d3NG", style = Typography.labelSmall)
                    }
                }
                TacticalCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${stats.packetsReceived}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = ConnectedGreen)
                        Text("\u0110\u00c3 NH\u1eacN", style = Typography.labelSmall)
                    }
                }
                TacticalCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${stats.heartbeatsSent}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = DispatchBlue)
                        Text("HEARTBEAT", style = Typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Retry & Drop Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TacticalCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${stats.retryAttempts}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = HighAmber)
                        Text("RETRY", style = Typography.labelSmall)
                    }
                }
                TacticalCard(modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("${stats.packetsDropped}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CriticalRed)
                        Text("DROP", style = Typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("C\u00c1C NODE L\u00c1NG GI\u1ec0NG (${p2pState.discoveredPeers.size})", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TextSecondary))
            Spacer(modifier = Modifier.height(8.dp))

            if (p2pState.discoveredPeers.isEmpty()) {
                TacticalCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = RelayPurple, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("\u0110ang qu\u00e9t t\u00ecm c\u00e1c thi\u1ebft b\u1ecb l\u00e2n c\u1eadn...", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(p2pState.discoveredPeers) { peer ->
                        TacticalCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ConnectedGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(peer.deviceName ?: "Unknown", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(peer.deviceAddress ?: "", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ConnectedGreen.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ConnectedGreen)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { repository.wifiDirectManager.discoverPeers() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkElevated),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("QU\u00c9T L\u1ea0I", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { scope.launch { repository.retryFailedPackets() } },
                    colors = ButtonDefaults.buttonColors(containerColor = HighAmber.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, tint = HighAmber)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RETRY FAIL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HighAmber)
                }
            }
        }
    }
}