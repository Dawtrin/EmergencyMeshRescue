package com.rescue.mesh.android.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescue.mesh.android.data.local.UserPreferences
import com.rescue.mesh.android.data.model.MeshPacket
import com.rescue.mesh.android.data.repository.MeshRepository
import com.rescue.mesh.android.hardware.FlashlightManager
import com.rescue.mesh.android.hardware.GpsLocationManager
import com.rescue.mesh.android.hardware.SirenSoundPlayer
import com.rescue.mesh.android.ui.components.LiveGpsBadge
import com.rescue.mesh.android.ui.components.PulseSosButton
import com.rescue.mesh.android.ui.components.StatusPill
import com.rescue.mesh.android.ui.components.TacticalCard
import com.rescue.mesh.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SosHomeScreen(
    repository: MeshRepository,
    flashlightManager: FlashlightManager,
    sirenPlayer: SirenSoundPlayer,
    onNavigateToCompass: () -> Unit,
    onNavigateToRelay: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val profile by repository.userPreferences.userProfileFlow.collectAsState(initial = UserPreferences.UserProfile())
    val p2pState by repository.wifiDirectManager.p2pState.collectAsState()
    val isBroadcasting by repository.isSosBroadcasting.collectAsState()
    val activeDispatch by repository.activeDispatchCmd.collectAsState()

    var currentLocation by remember { mutableStateOf<GpsLocationManager.LocationData?>(null) }
    var selectedAlertType by remember { mutableStateOf(MeshPacket.ALERT_FLOOD) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isSirenOn by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.gpsManager.getLocationUpdates().collect { loc -> currentLocation = loc }
    }
    LaunchedEffect(profile.relayPort) {
        repository.startListeningServer(profile.relayPort)
    }

    fun lightClickHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            }
        } catch (_: Exception) {}
    }

    fun triggerSos() {
        scope.launch {
            val result = repository.broadcastSos(
                alertType = selectedAlertType,
                severity = MeshPacket.SEVERITY_CRITICAL,
                message = "",
                victimCount = profile.victimCount,
                location = currentLocation
            )
            if (result.success) {
                Toast.makeText(context, "\ud83d\udea8 \u0110\u00c3 PH\u00c1T T\u00cdN HI\u1ec6U SOS TH\u00c0NH C\u00d4NG!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "\u26a0\ufe0f L\u1ed7i truy\u1ec1n tin: ${result.errorMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(OledBlack)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Status Pill + Action Buttons (History & Config)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(nodeMode = "N\u1ea0N NH\u00c2N", neighborCount = p2pState.discoveredPeers.size, isRelayRunning = false)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { onNavigateToHistory() },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(DarkElevated)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { showConfigDialog = true },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(DarkElevated)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Config", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            LiveGpsBadge(location = currentLocation)
            Spacer(modifier = Modifier.height(16.dp))

            // 4 Loại tình huống khẩn cấp
            TacticalCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("LO\u1ea0I T\u00ccNH HU\u1ed0NG KH\u1ea8N C\u1ea4P", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = HighAmber))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        data class AlertOption(val type: String, val emoji: String, val label: String, val color: Color)
                        val types = listOf(
                            AlertOption(MeshPacket.ALERT_FLOOD, "\ud83c\udf0a", "L\u0169 l\u1ee5t", CriticalRed),
                            AlertOption(MeshPacket.ALERT_MEDICAL, "\ud83c\udfe5", "Y t\u1ebf", ConnectedGreen),
                            AlertOption(MeshPacket.ALERT_LANDSLIDE, "\u26f0\ufe0f", "S\u1ea1t l\u1edf", HighAmber),
                            AlertOption(MeshPacket.ALERT_EARTHQUAKE, "\ud83c\udf0b", "\u0110\u1ed9ng \u0111\u1ea5t", WarningYellow)
                        )
                        types.forEach { opt ->
                            val isSel = selectedAlertType == opt.type
                            val selScale by animateFloatAsState(
                                targetValue = if (isSel) 1f else 0.95f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "sel_${opt.type}"
                            )
                            Box(
                                modifier = Modifier.weight(1f).scale(selScale)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) opt.color.copy(alpha = 0.25f) else DarkElevated)
                                    .clickable { lightClickHaptic(); selectedAlertType = opt.type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(opt.emoji, fontSize = 18.sp)
                                    Text(opt.label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium, color = if (isSel) opt.color else TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HERO SOS BUTTON
            PulseSosButton(
                isBroadcasting = isBroadcasting,
                onSosTriggered = { triggerSos() },
                onQuickSos = { triggerSos() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Survival Actions Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TacticalCard(
                    modifier = Modifier.weight(1f).clickable { flashlightManager.toggleMorseSos { isFlashOn = it } },
                    backgroundColor = if (isFlashOn) WarningYellow.copy(alpha = 0.25f) else GlassSurface
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = "Flash", tint = if (isFlashOn) WarningYellow else Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isFlashOn) "\u0110\u00c8N SOS: B\u1eacT" else "\u0110\u00c8N MORSE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isFlashOn) WarningYellow else Color.White)
                    }
                }
                TacticalCard(
                    modifier = Modifier.weight(1f).clickable { sirenPlayer.toggleSiren { isSirenOn = it } },
                    backgroundColor = if (isSirenOn) CriticalRed.copy(alpha = 0.25f) else GlassSurface
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Siren", tint = if (isSirenOn) CriticalRed else Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(if (isSirenOn) "C\u00d2I H\u00da: B\u1eacT" else "C\u00d2I C\u1ee8U N\u1ea0N", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSirenOn) CriticalRed else Color.White)
                    }
                }
                TacticalCard(
                    modifier = Modifier.weight(1f).clickable { onNavigateToCompass() },
                    backgroundColor = GlassSurface
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Explore, contentDescription = "Compass", tint = ConnectedGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("LA B\u00c0N", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ConnectedGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onNavigateToRelay() },
                colors = ButtonDefaults.buttonColors(containerColor = RelayPurple.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Router, contentDescription = null, tint = RelayPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CHUY\u1ec2N SANG TI\u1ebeP S\u00d3NG (RELAY)", color = RelayPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        DispatchAlertOverlay(
            dispatchPacket = activeDispatch,
            onAcknowledge = { pkt -> scope.launch {
                repository.sendAckForDispatch(pkt)
                Toast.makeText(context, "\u0110\u00e3 g\u1eedi ph\u1ea3n h\u1ed3i ACK v\u1ec1 Tr\u1ea1m!", Toast.LENGTH_SHORT).show()
            }},
            onDismiss = { repository.dismissDispatchAlert() }
        )
    }

    if (showConfigDialog) {
        var baseIp by remember { mutableStateOf(profile.baseStationIp) }
        var basePort by remember { mutableStateOf(profile.baseStationPort.toString()) }
        var relayPort by remember { mutableStateOf(profile.relayPort.toString()) }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("C\u1ea5u h\u00ecnh M\u1ea1ng C\u1ee9u H\u1ed9", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = baseIp, onValueChange = { baseIp = it }, label = { Text("IP Base Station / Relay") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = basePort, onValueChange = { basePort = it }, label = { Text("Port Base Station (8888)") }, singleLine = true)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = relayPort, onValueChange = { relayPort = it }, label = { Text("Port l\u1eafng nghe (8889)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { scope.launch {
                    repository.userPreferences.updateNetworkConfig(baseIp, basePort.toIntOrNull() ?: 8888, relayPort.toIntOrNull() ?: 8889)
                    showConfigDialog = false
                }}) { Text("L\u01b0u c\u1ea5u h\u00ecnh") }
            },
            dismissButton = { TextButton(onClick = { showConfigDialog = false }) { Text("H\u1ee7y") } }
        )
    }
}